package com.dream11.orchestrator.service;

import static com.dream11.orchestrator.constants.Constants.DEPLOY_ACTION_NAME;
import static com.dream11.orchestrator.constants.Constants.DISCOVERY_DELETE_ACTION_NAME;
import static com.dream11.orchestrator.constants.Constants.DISCOVERY_UPSERT_ACTION_NAME;
import static com.dream11.orchestrator.constants.Constants.ODIN_DISCOVERY_MARKER_END;
import static com.dream11.orchestrator.constants.Constants.ODIN_DISCOVERY_MARKER_START;
import static com.dream11.orchestrator.constants.Constants.UNDEPLOY_ACTION_NAME;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.DISCOVERY_OUTPUT_DOES_NOT_MATCH_INPUT;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.DISCOVERY_OUTPUT_NOT_FOUND_IN_LOGS;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.DISCOVERY_SERVICE_ERROR;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.INVALID_DISCOVERY_ACTION;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.dto.discovery.DiscoveryRecord;
import com.dream11.orchestrator.dto.discovery.DiscoveryRequestMessage;
import com.dream11.orchestrator.dto.discovery.RecordAction;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.util.DnsUtil;
import com.dream11.orchestrator.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class DiscoveryClientService {

  public static final String DISCOVERY_ENDPOINT = "/v1/record";
  final AppConfig appConfig;
  final HttpClient httpClient;

  public void handleDiscovery(
      long orgId, ComponentAction componentAction, String logs, String actionName)
      throws JsonProcessingException {
    switch (actionName) {
      case DEPLOY_ACTION_NAME:
        createRoute(orgId, componentAction, logs);
        break;
      case UNDEPLOY_ACTION_NAME:
        deleteRoute(orgId, componentAction);
        break;
      default:
    }
  }

  private void createRoute(long orgId, ComponentAction componentAction, String logs)
      throws JsonProcessingException {

    // Step 1: Get discovery output from logs
    String runnerDiscoveryOutput = getDiscoveryOutputFromLogs(logs);

    // Step 2: Check if discovery output contains CNAME records
    if (!containsCnameRecords(runnerDiscoveryOutput)) {
      return;
    }

    // Step 3: Get discovery from userdata
    String discoveryFromUserData =
        getDiscoveryFromUserData(
            AppContext.getObjectMapper().writeValueAsString(componentAction.getBaseConfig()));

    log.info("Discovery from userdata: {}", discoveryFromUserData);
    // Step 5: Compare discovery and discovery output
    if (!discoveryFromUserData.isEmpty()) {
      compareOutputsAndCreateRecords(
          discoveryFromUserData,
          runnerDiscoveryOutput,
          componentAction.getAccounts().getAccount().getName(),
          orgId);
    }
  }

  private void deleteRoute(long orgId, ComponentAction componentAction)
      throws JsonProcessingException {
    // Step 1: Get discovery from userdata
    String discoveryFromUserData =
        getDiscoveryFromUserData(
            AppContext.getObjectMapper().writeValueAsString(componentAction.getBaseConfig()));

    if (!discoveryFromUserData.isEmpty()) {
      traverseDiscoveryAndDeleteRecords(
          AppContext.getObjectMapper().readTree(discoveryFromUserData),
          componentAction.getAccounts().getAccount().getName(),
          orgId);
    }
  }

  void traverseDiscoveryAndDeleteRecords(
      JsonNode discoveryFromUserData, String accountName, long orgId) {
    if (discoveryFromUserData.isObject()) {
      discoveryFromUserData
          .fieldNames()
          .forEachRemaining(
              key ->
                  traverseDiscoveryAndDeleteRecords(
                      discoveryFromUserData.get(key), accountName, orgId));
    } else if (discoveryFromUserData.isArray()) {
      Set<String> keySet = new HashSet<>();
      discoveryFromUserData.forEach(node -> keySet.add(node.asText()));
      callDiscoveryService(keySet, "", accountName, orgId, DISCOVERY_DELETE_ACTION_NAME);
    } else {
      callDiscoveryService(
          discoveryFromUserData.asText(), "", accountName, orgId, 1, DISCOVERY_DELETE_ACTION_NAME);
    }
  }

  protected String getDiscoveryOutputFromLogs(String logs) {
    String output =
        getContentBetweenMarkers(logs, ODIN_DISCOVERY_MARKER_START, ODIN_DISCOVERY_MARKER_END);

    if (output == null || output.isEmpty()) {
      throw new OrchestratorException(DISCOVERY_OUTPUT_NOT_FOUND_IN_LOGS);
    }

    return output;
  }

  protected String getContentBetweenMarkers(String content, String startMarker, String endMarker) {
    String regex = String.format("(%s)(.*?)(%s)", startMarker, endMarker);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(content);

    if (matcher.find()) {
      return matcher.group(2);
    } else {
      log.warn("No string found between markers {} and {}", startMarker, endMarker);
      return "";
    }
  }

  protected boolean containsCnameRecords(String discoveryOutput) throws JsonProcessingException {
    JsonNode rootNode = AppContext.getObjectMapper().readTree(discoveryOutput);
    return containsCnameRecordsRecursive(rootNode);
  }

  private boolean containsCnameRecordsRecursive(JsonNode node) {
    // NOTE: Doesn't handle heterogeneous jsons, i.e. one which contains both cname and ip address

    if (node.isObject()) {
      Iterator<JsonNode> elements = node.elements();
      while (elements.hasNext()) {
        if (!containsCnameRecordsRecursive(elements.next())) {
          return false;
        }
      }
      return true;
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        if (!containsCnameRecordsRecursive(element)) {
          return false;
        }
      }
      return true;
    } else if (node.isTextual()) {
      String[] values = node.asText().split("\\s*,\\s*");
      return Arrays.stream(values)
          .map(this::isIpAddress)
          .filter(Boolean::booleanValue)
          .toList()
          .isEmpty();
      // Any non ip value is assumed to correspond to CNAME discoveryRecord
    }
    return false;
  }

  protected boolean isIpAddress(String value) {
    return value != null
        && value.matches(
            "\\b((25[0-5]|2[0-4][\\d]|[01]?[\\d][\\d]?)\\.){3}(25[0-5]|2[0-4][\\d]|[01]?[\\d][\\d]?)\\b");
  }

  protected String getDiscoveryFromUserData(String userData) throws JsonProcessingException {
    JsonNode rootNode = AppContext.getObjectMapper().readTree(userData);
    JsonNode discoveryNode = rootNode.path("discovery");
    if (discoveryNode.isMissingNode()) {
      return "";
    }
    return discoveryNode.toString();
  }

  @SneakyThrows
  protected void compareOutputsAndCreateRecords(
      String discoveryFromUserdata, String runnerDiscoveryOutput, String accountName, long orgId) {
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(discoveryFromUserdata);
    JsonNode runnerDiscoveryOutputNode =
        AppContext.getObjectMapper().readTree(runnerDiscoveryOutput);
    compareDiscoveryNodesAndCreateRecords(
        userDataDiscoveryNode, runnerDiscoveryOutputNode, accountName, orgId);
  }

  protected void compareDiscoveryNodesAndCreateRecords(
      JsonNode userDataDiscoveryNode,
      JsonNode runnerDiscoveryOutputNode,
      String accountName,
      long orgId) {
    if (userDataDiscoveryNode.isObject() && runnerDiscoveryOutputNode.isObject()) {
      iterateFields(
          userDataDiscoveryNode.fieldNames(),
          userDataDiscoveryNode,
          runnerDiscoveryOutputNode,
          accountName,
          orgId);
    } else if (userDataDiscoveryNode.isValueNode() && runnerDiscoveryOutputNode.isValueNode()) {
      if (!userDataDiscoveryNode.asText().equals(runnerDiscoveryOutputNode.asText())) {
        callDiscoveryService(
            userDataDiscoveryNode.asText(),
            runnerDiscoveryOutputNode.asText(),
            accountName,
            orgId,
            1,
            DISCOVERY_UPSERT_ACTION_NAME);
      } else {
        log.info("Discovery output and userdata discovery are same");
      }
    } else if (userDataDiscoveryNode.isArray() && runnerDiscoveryOutputNode.isValueNode()) {
      Set<String> keySet = new HashSet<>();
      userDataDiscoveryNode.forEach(
          node -> {
            if (!node.textValue().equals(runnerDiscoveryOutputNode.textValue()))
              keySet.add(node.asText());
          });
      callDiscoveryService(
          keySet,
          runnerDiscoveryOutputNode.asText(),
          accountName,
          orgId,
          DISCOVERY_UPSERT_ACTION_NAME);
    } else {
      throw new OrchestratorException(DISCOVERY_OUTPUT_DOES_NOT_MATCH_INPUT);
    }
  }

  private void iterateFields(
      Iterator<String> fieldNames,
      JsonNode userDataDiscoveryNode,
      JsonNode runnerDiscoveryOutputNode,
      String accountName,
      long orgId) {
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode discoveryChildNode = userDataDiscoveryNode.get(fieldName);
      JsonNode discoveryOutputChildNode = runnerDiscoveryOutputNode.get(fieldName);
      if (discoveryChildNode == null || discoveryOutputChildNode == null) {
        throw new OrchestratorException(DISCOVERY_OUTPUT_DOES_NOT_MATCH_INPUT);
      }
      compareDiscoveryNodesAndCreateRecords(
          discoveryChildNode, discoveryOutputChildNode, accountName, orgId);
    }
  }

  private void callDiscoveryService(
      Set<String> keys, String value, String accountName, long orgId, String action) {
    List<String> keysList = new ArrayList<>(keys);
    for (int i = 0; i < keysList.size(); i++)
      callDiscoveryService(keysList.get(i), value, accountName, orgId, i + 1, action);
  }

  private void callDiscoveryService(
      String key, String value, String accountName, long orgId, int id, String action) {
    log.info(
        "Calling discovery service for key: {}, value: {}, accountName: {}, orgId: {}",
        key,
        value,
        accountName,
        orgId);
    try (StringEntity entity =
        new StringEntity(
            AppContext.getObjectMapper()
                .writeValueAsString(createDiscoveryRequest(key, value, accountName, id, action)),
            ContentType.APPLICATION_JSON)) {
      HttpPut request =
          new HttpPut(
              new URIBuilder(appConfig.getDiscovery().getUrl())
                  .setPath(DISCOVERY_ENDPOINT)
                  .build());
      request.setHeader("Content-Type", "application/json");
      request.setHeader("orgId", orgId);
      request.setEntity(entity);
      httpClient.execute(
          request,
          response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode >= 400) {
              throw new OrchestratorException(
                  DISCOVERY_SERVICE_ERROR,
                  "service returned status code: " + statusCode + " response: " + responseBody);
            }
            if (statusCode == 200) {
              String errorMsg = checkForFailures(responseBody);
              if (!errorMsg.isEmpty()) {
                throw new OrchestratorException(DISCOVERY_SERVICE_ERROR, errorMsg);
              }
            }
            return response;
          });
    } catch (IOException | URISyntaxException e) {
      log.error(
          "Error while calling discovery service for key: {}, value: {}, accountName: {}, orgId: {} ,\ngot response:{}",
          key,
          value,
          accountName,
          orgId,
          e.getMessage());
      throw new OrchestratorException(DISCOVERY_SERVICE_ERROR, e.getMessage());
    }
  }

  private static String checkForFailures(String responseBody) throws JsonProcessingException {
    JsonNode root = AppContext.getObjectMapper().readTree(responseBody);
    JsonNode responseList = root.path("responseList");
    StringBuilder message = new StringBuilder();
    if (responseList.isArray()) {
      for (JsonNode item : responseList) {
        String status = item.path("status").asText();
        if ("FAILED".equalsIgnoreCase(status)) {
          message.append(item.path("message").asText()).append("\n");
        }
      }
    }
    return message.toString();
  }

  private DiscoveryRequestMessage createDiscoveryRequest(
      String key, String value, String accountName, int id, String action) {
    DiscoveryRecord discoveryRecord;
    if (DISCOVERY_UPSERT_ACTION_NAME.equalsIgnoreCase(action)) {
      discoveryRecord = DiscoveryRecord.builder().name(key).values(List.of(value)).build();
    } else if (DISCOVERY_DELETE_ACTION_NAME.equalsIgnoreCase(action)) {
      discoveryRecord = DiscoveryRecord.builder().name(key).build();
    } else throw new OrchestratorException(INVALID_DISCOVERY_ACTION);

    return DiscoveryRequestMessage.builder()
        .accountName(accountName)
        .recordActions(
            List.of(
                RecordAction.builder()
                    .action(action)
                    .id(String.valueOf(id))
                    .discoveryRecord(discoveryRecord)
                    .build()))
        .build();
  }

  @SneakyThrows
  public boolean doesDNSResolveCorrectly(
      JsonNode runnerDiscoveryOutputNode, JsonNode userDataDiscoveryNode) {
    List<Pair<String, String>> endpoints =
        JsonUtil.extractMatchingLeafNodes(runnerDiscoveryOutputNode, userDataDiscoveryNode);
    List<Pair<String, String>> endpointsCreatedByOdin =
        endpoints.stream()
            .filter(pair -> !pair.getLeft().equalsIgnoreCase(pair.getRight()))
            .toList();

    Set<String> successfullyResolvedDns = new HashSet<>();

    for (int counter = 1; counter < this.appConfig.getHealthcheck().getMaxRetries(); counter++) {
      for (Pair<String, String> stringStringPair : endpointsCreatedByOdin) {
        if (DnsUtil.isDnsResolvableToTarget(
            stringStringPair.getRight(),
            Arrays.stream(stringStringPair.getLeft().split(",")).collect(Collectors.toSet()))) {
          successfullyResolvedDns.add(stringStringPair.getRight());
        }
      }
      if (successfullyResolvedDns.size() == endpointsCreatedByOdin.size()) {
        break;
      }
      Thread.sleep(this.appConfig.getHealthcheck().getWaitSeconds() * 1000L);
    }
    if (successfullyResolvedDns.size() != endpointsCreatedByOdin.size()) {
      log.error(
          "DNS verification failed for {}",
          endpointsCreatedByOdin.stream()
              .map(Pair::getRight)
              .collect(Collectors.toSet())
              .removeAll(successfullyResolvedDns));
      return false;
    }
    return true;
  }
}
