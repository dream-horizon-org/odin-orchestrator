package com.dream11.orchestrator.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

    assertEquals("{\"key\":\"value\"}", result);
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

    assertEquals(content, result);
  }

  @Test
  void testContainsCnameRecordsWithCname() throws JsonProcessingException {
    String discoveryOutput = "{\"key\":\"cnameRecord\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertTrue(result);
  }

  @Test
  void testContainsCnameRecordsWithIp() throws JsonProcessingException {
    String discoveryOutput = "{\"key\":\"192.168.1.1\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertFalse(result);
  }

  @Test
  void testContainsCnameRecordsWithMixedContent() throws JsonProcessingException {
    String discoveryOutput = "{\"key1\":\"cnameRecord\", \"key2\":\"192.168.1.1\"}";

    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    assertFalse(result);
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

    assertTrue(discoveryClientService.isIpAddress(validIp1));
    assertTrue(discoveryClientService.isIpAddress(validIp2));
    assertTrue(discoveryClientService.isIpAddress(validIp3));
  }

  @Test
  void testIsIpAddressInvalidIp() {
    String invalidIp1 = "256.256.256.256";
    String invalidIp2 = "192.168.1";
    String invalidIp3 = "192.168.1.1.1";
    String invalidIp4 = "192.168.1.a";
    String invalidIp5 = "192.168.1.256";
    String invalidIp6 = "300.300.300.300";

    assertFalse(discoveryClientService.isIpAddress(invalidIp1));
    assertFalse(discoveryClientService.isIpAddress(invalidIp2));
    assertFalse(discoveryClientService.isIpAddress(invalidIp3));
    assertFalse(discoveryClientService.isIpAddress(invalidIp4));
    assertFalse(discoveryClientService.isIpAddress(invalidIp5));
    assertFalse(discoveryClientService.isIpAddress(invalidIp6));
  }

  @Test
  void testIsIpAddressEdgeCases() {
    String emptyString = "";
    String nullString = null;
    String whitespaceString = "   ";
    String nonIpString = "not.an.ip.address";

    assertFalse(discoveryClientService.isIpAddress(emptyString));
    assertFalse(discoveryClientService.isIpAddress(nullString));
    assertFalse(discoveryClientService.isIpAddress(whitespaceString));
    assertFalse(discoveryClientService.isIpAddress(nonIpString));
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

    assertEquals(discoveryOutput, result);
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
        "{\n" + "      \"key\" : {\n" + "        \"key1\" : \"value1\"\n" + "      }\n" + "    }";

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
    String runnerDataJson = "{\n" + "      \"key\": [\"value1\", \"value2\"]\n" + "    }";

    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }
}
