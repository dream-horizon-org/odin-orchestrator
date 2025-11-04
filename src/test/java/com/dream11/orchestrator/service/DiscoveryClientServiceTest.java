package com.dream11.orchestrator.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscoveryClientServiceTest {

  @InjectMocks private DiscoveryClientService discoveryClientService;

  @Test
  void testGetDiscoveryOutputFromLogsValid() {
    String logs =
        "------ODIN-DISCOVERY-MARKER-START------\n{\"key\":\"value\"}\n------ODIN-DISCOVERY-MARKER-END------";

    String result = discoveryClientService.getDiscoveryOutputFromLogs(logs);

    assertThat("{\"key\":\"value\"}").isEqualTo(result);
  }

  @Test
  void testGetDiscoveryOutputFromLogsNoContent() {
    String logs = "invalidLogs";

    assertThatThrownBy(() -> discoveryClientService.getDiscoveryOutputFromLogs(logs))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("No discovery output found in logs");
  }

  @ParameterizedTest
  @CsvSource({
    "startMarkerSomeContentEndMarker,SomeContent",
    "startMarkerEndMarker,''",
    "SomeContentEndMarker,''",
    "startMarkerSomeContent,''",
    "'',''",
    "startMarkerContent1EndMarkerstartMarkerContent2EndMarker,Content1"
  })
  void testGetContentBetweenMarkers(String rawContent, String content) {
    String startMarker = "startMarker";
    String endMarker = "EndMarker";

    String result =
        discoveryClientService.getContentBetweenMarkers(rawContent, startMarker, endMarker);

    assertThat(content).isEqualTo(result);
  }

  @Test
  void testContainsCnameRecordsWithCname() throws JsonProcessingException {
    String discoveryOutput = "{\"key\":\"cnameRecord\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isTrue();
  }

  @Test
  void testContainsCnameRecordsWithIp() throws JsonProcessingException {
    String discoveryOutput = "{\"key\":\"192.168.1.1\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsWithMixedContent() throws JsonProcessingException {
    String discoveryOutput = "{\"key1\":\"cnameRecord\", \"key2\":\"192.168.1.1\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsInvalidJson() {
    String discoveryOutput = "192.168.1.1,192.168.1.2";

    assertThatThrownBy(() -> discoveryClientService.containsCnameRecords(discoveryOutput))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void testContainsCnameRecordsMultipleIps() throws JsonProcessingException {
    String discoveryOutput = "{\"endpoints\":\" 10.103.202.164 , 10.103.202.164\"}";

    assertThat(discoveryClientService.containsCnameRecords(discoveryOutput)).isFalse();
  }

  @Test
  void testIsIpAddressValidIp() {
    String validIp1 = "192.168.1.1";
    String validIp2 = "0.0.0.0";
    String validIp3 = "255.255.255.255";

    assertThat(discoveryClientService.isIpAddress(validIp1)).isTrue();
    assertThat(discoveryClientService.isIpAddress(validIp2)).isTrue();
    assertThat(discoveryClientService.isIpAddress(validIp3)).isTrue();
  }

  @Test
  void testIsIpAddressInvalidIp() {
    String invalidIp1 = "256.256.256.256";
    String invalidIp2 = "192.168.1";
    String invalidIp3 = "192.168.1.1.1";
    String invalidIp4 = "192.168.1.a";
    String invalidIp5 = "192.168.1.256";
    String invalidIp6 = "300.300.300.300";

    assertThat(discoveryClientService.isIpAddress(invalidIp1)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp2)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp3)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp4)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp5)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp6)).isFalse();
  }

  @Test
  void testIsIpAddressEdgeCases() {
    String emptyString = "";
    String nullString = null;
    String whitespaceString = "   ";
    String nonIpString = "not.an.ip.address";

    assertThat(discoveryClientService.isIpAddress(emptyString)).isFalse();
    assertThat(discoveryClientService.isIpAddress(nullString)).isFalse();
    assertThat(discoveryClientService.isIpAddress(whitespaceString)).isFalse();
    assertThat(discoveryClientService.isIpAddress(nonIpString)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "{\"discovery\":{\"key\":\"value\"}},{\"key\":\"value\"}",
    "{\"otherData\":{\"key\":\"value\"}},''",
    "{\"discovery\":{}},{}"
  })
  void testGetDiscoveryFromUserDataWithDiscovery(String userData, String discoveryOutput)
      throws JsonProcessingException {
    String result = discoveryClientService.getDiscoveryFromUserData(userData);

    assertThat(discoveryOutput).isEqualTo(result);
  }

  @Test
  void testGetDiscoveryFromUserDataWithInvalidJson() {
    String userData = "invalidJson";

    assertThatThrownBy(() -> discoveryClientService.getDiscoveryFromUserData(userData))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedNodes() throws Exception {
    String userDataJson = "{\"key\":\"value1\"}";
    String runnerDataJson = "{\"key2\":\"value2\"}";

    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedTypes() throws Exception {
    String userDataJson = "{\"key\":\"value1\"}";
    String runnerDataJson =
        new JSONObject(
                """
                        {
                          "key": {
                              "key1" : "value1"
                          }
                        }
                        """)
            .toString();

    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedTypesWithArrays() throws Exception {
    String userDataJson = "{\"key\":\"value1\"}";
    String runnerDataJson =
        new JSONObject(
                """
                        {
                          "key": ["value1", "value2"]
                        }
                        """)
            .toString();
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testContainsCnameRecordsAsArrayFalse() throws JsonProcessingException {
    String discoveryOutput = "[\"192.168.1.1\"]";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsAsArrayTrue() throws JsonProcessingException {
    String discoveryOutput = "[\"key\"]";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isTrue();
  }

  @Test
  void testContainsCnameRecordsAsTextual() throws JsonProcessingException {
    String discoveryOutput = "false";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertThat(result).isFalse();
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithMismatchedTypes() throws Exception {
    String userDataJson = "{\"key\":\"value1\"}";
    String runnerDataJson = "[]";
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithMismatchedNodesWithValueTypes() throws Exception {
    String userDataJson = "[\"key\"]";
    String runnerDataJson = "[]";
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithArrayAndTextualInput() throws Exception {
    String userDataJson = "[\"key\"]";
    String runnerDataJson = "\"key\"";
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    discoveryClientService.compareDiscoveryNodesAndCreateRecords(
        userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1);
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithSameTextualValue() throws Exception {
    String userDataJson = "\"key\"";
    String runnerDataJson = "\"key\"";
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    discoveryClientService.compareDiscoveryNodesAndCreateRecords(
        userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1);
  }
}
