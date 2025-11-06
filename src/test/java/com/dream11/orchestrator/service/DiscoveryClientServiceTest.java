package com.dream11.orchestrator.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
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
    // Arrange
    String logs =
        "------ODIN-DISCOVERY-MARKER-START------\n{\"key\":\"value\"}\n------ODIN-DISCOVERY-MARKER-END------";

    // Act
    String result = discoveryClientService.getDiscoveryOutputFromLogs(logs);

    // Assert
    assertThat(result).isEqualTo("{\"key\":\"value\"}");
  }

  @Test
  void testGetDiscoveryOutputFromLogsNoContent() {
    // Arrange
    String logs = "invalidLogs";

    // Assert
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
    // Arrange
    String startMarker = "startMarker";
    String endMarker = "EndMarker";

    // Act
    String result =
        discoveryClientService.getContentBetweenMarkers(rawContent, startMarker, endMarker);

    // Assert
    assertThat(result).isEqualTo(content);
  }

  @Test
  void testContainsCnameRecordsWithCname() throws JsonProcessingException {
    // Arrange
    String discoveryOutput = new JSONObject().put("key", "cnameRecord").toString();

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void testContainsCnameRecordsWithIp() throws JsonProcessingException {
    // Arrange
    String discoveryOutput = new JSONObject().put("key", "192.168.1.1").toString();

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsWithMixedContent() throws JsonProcessingException {
    // Arrange
    String discoveryOutput =
        new JSONObject().put("key1", "cnameRecord").put("key2", "192.168.1.1").toString();

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsInvalidJson() {
    // Arrange
    String discoveryOutput = "192.168.1.1,192.168.1.2";

    // Assert
    assertThatThrownBy(() -> discoveryClientService.containsCnameRecords(discoveryOutput))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void testContainsCnameRecordsMultipleIps() throws JsonProcessingException {
    // Arrange
    String discoveryOutput =
        new JSONObject().put("endpoints", " 10.103.202.164 , 10.103.202.164").toString();

    // Act && Assert
    assertThat(discoveryClientService.containsCnameRecords(discoveryOutput)).isFalse();
  }

  @Test
  void testIsIpAddressValidIp() {
    // Arrange
    String validIp1 = "192.168.1.1";
    String validIp2 = "0.0.0.0";
    String validIp3 = "255.255.255.255";

    // Assert
    assertThat(discoveryClientService.isIpAddress(validIp1)).isTrue();
    assertThat(discoveryClientService.isIpAddress(validIp2)).isTrue();
    assertThat(discoveryClientService.isIpAddress(validIp3)).isTrue();
  }

  @Test
  void testIsIpAddressInvalidIp() {
    // Arrange
    String invalidIp1 = "256.256.256.256";
    String invalidIp2 = "192.168.1";
    String invalidIp3 = "192.168.1.1.1";
    String invalidIp4 = "192.168.1.a";
    String invalidIp5 = "192.168.1.256";
    String invalidIp6 = "300.300.300.300";

    // Act && Assert
    assertThat(discoveryClientService.isIpAddress(invalidIp1)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp2)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp3)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp4)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp5)).isFalse();
    assertThat(discoveryClientService.isIpAddress(invalidIp6)).isFalse();
  }

  @Test
  void testIsIpAddressEdgeCases() {
    // Arrange
    String emptyString = "";
    String nullString = null;
    String whitespaceString = "   ";
    String nonIpString = "not.an.ip.address";

    // Act && Assert
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
    // Act
    String result = discoveryClientService.getDiscoveryFromUserData(userData);

    // Assert
    assertThat(result).isEqualTo(discoveryOutput);
  }

  @Test
  void testGetDiscoveryFromUserDataWithInvalidJson() {
    // Arrange
    String userData = "invalidJson";

    // Act && Assert
    assertThatThrownBy(() -> discoveryClientService.getDiscoveryFromUserData(userData))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedNodes() throws Exception {
    // Arrange
    String userDataJson = new JSONObject().put("key", "value1").toString();
    String runnerDataJson = new JSONObject().put("key2", "value2").toString();

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Assert
    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedTypes() throws Exception {
    // Arrange
    String userDataJson = new JSONObject().put("key", "value1").toString();
    String runnerDataJson =
        new JSONObject().put("name", new JSONObject().put("key1", "value1")).toString();

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Assert
    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareDiscoveryNodesAndCreateRecordsWithMismatchedTypesWithArrays() throws Exception {
    // Arrange
    String userDataJson = new JSONObject().put("key", "value1").toString();
    String runnerDataJson = new JSONObject().put("key", List.of("value1", "value2")).toString();

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Assert
    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testContainsCnameRecordsAsArrayFalse() throws JsonProcessingException {
    // Arrange
    String discoveryOutput = "[\"192.168.1.1\"]";

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void testContainsCnameRecordsAsArrayTrue() throws JsonProcessingException {
    // Arrange
    String discoveryOutput = "[\"key\"]";

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void testContainsCnameRecordsAsTextual() throws JsonProcessingException {
    // Arrange
    String discoveryOutput = "false";

    // Act
    boolean result = discoveryClientService.containsCnameRecords(discoveryOutput);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithMismatchedTypes() throws Exception {
    // Arrange
    String userDataJson = new JSONObject().put("key", "value1").toString();
    String runnerDataJson = "[]";

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Assert
    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithMismatchedNodesWithValueTypes() throws Exception {
    // Arrange
    String userDataJson = "[\"key\"]";
    String runnerDataJson = "[]";

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Assert
    assertThatThrownBy(
            () ->
                discoveryClientService.compareDiscoveryNodesAndCreateRecords(
                    userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Discovery output doesn't match with input");
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithArrayAndTextualInput() throws Exception {
    // Arrange
    String userDataJson = "[\"key\"]";
    String runnerDataJson = "\"key\"";

    // Act
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    discoveryClientService.compareDiscoveryNodesAndCreateRecords(
        userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1);

    // Assert
    assertThat(discoveryClientService.containsCnameRecords("\"key\"")).isTrue();
  }

  @Test
  void testCompareOutputsAndCreateRecordsWithSameTextualValue() throws Exception {
    // Arrange
    String userDataJson = "\"key\"";
    String runnerDataJson = "\"key\"";
    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(userDataJson);
    JsonNode runnerDiscoveryOutputNode = AppContext.getObjectMapper().readTree(runnerDataJson);

    // Act
    discoveryClientService.compareDiscoveryNodesAndCreateRecords(
        userDataDiscoveryNode, runnerDiscoveryOutputNode, "staging", 1);

    // Assert
    assertThat(discoveryClientService.containsCnameRecords("\"key\"")).isTrue();
  }
}
