package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

  private static final ObjectMapper OBJECT_MAPPER = AppContext.getObjectMapper();

  @Test
  void testExtractMatchingLeafNodesBaseCase() throws JsonProcessingException {
    // Arrange
    JsonNode node1 = OBJECT_MAPPER.readTree("{\"key1\":\"value1\"}");
    JsonNode node2 = OBJECT_MAPPER.readTree("{\"key1\":\"value2\"}");
    JsonNode node3 = OBJECT_MAPPER.readTree("");

    // Act
    List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);

    // Assert
    assertThat(JsonUtil.extractMatchingLeafNodes(node1, node2).size()).isEqualTo(1);
    assertThat(pairs.get(0).getLeft()).isEqualTo("value1");
    assertThat(pairs.get(0).getRight()).isEqualTo("value2");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node2, node3))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[{\"key1\":\"value2\"}] and node2[null] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesNestedJson() throws JsonProcessingException {
    // Arrange
    JsonNode node1 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"xyz.com\",\"private\":\"xyz.local\"}}");
    JsonNode node2 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"pqr.com\",\"private\":\"pqr.local\"}}");

    // Act
    List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);

    // Assert
    assertThat(JsonUtil.extractMatchingLeafNodes(node1, node2).size()).isEqualTo(2);
    assertThat(pairs.get(0).getLeft()).isEqualTo("xyz.com");
    assertThat(pairs.get(0).getRight()).isEqualTo("pqr.com");
    assertThat(pairs.get(1).getLeft()).isEqualTo("xyz.local");
    assertThat(pairs.get(1).getRight()).isEqualTo("pqr.local");
  }

  @Test
  void testExtractMatchingLeafNodesDifferentTrees() throws JsonProcessingException {
    // Arrange
    JsonNode node1 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"xyz.com\",\"private\":\"xyz.local\"}}");
    JsonNode node2 = OBJECT_MAPPER.readTree("{\"discovery\":{\"noMatch\":\"pqr.com\"}}");

    // Assert
    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Json structure of nodes node1[public] and node2[] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesArrayInput() throws JsonProcessingException {
    // Arrange
    JsonNode node1 = OBJECT_MAPPER.readTree("[\"key\"]");
    JsonNode node2 = OBJECT_MAPPER.readTree("[\"key\", \"value1\"]");
    JsonNode node3 = OBJECT_MAPPER.readTree("[\"key\", \"value2\"]");
    JsonNode objectNode = OBJECT_MAPPER.readTree("{\"discovery\":{\"public\":\"xyz.com\"}}");

    // Assert
    assertThat(JsonUtil.extractMatchingLeafNodes(node1, node1).size()).isEqualTo(1);

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\"]] and node2[[\"key\",\"value1\"]] does not match.");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node2, node3))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\",\"value1\"]] and node2[[\"key\",\"value2\"]] does not match.");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, objectNode))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\"]] and node2[{\"discovery\":{\"public\":\"xyz.com\"}}] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesNullInput() throws JsonProcessingException {
    // Arrange
    JsonNode node1 = null;
    JsonNode node2 = OBJECT_MAPPER.readTree("");

    // Act
    List<Pair<String, String>> nullPairs = JsonUtil.extractMatchingLeafNodes(node1, node1);

    // Assert
    assertThat(nullPairs.size()).isEqualTo(0);

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[null] and node2[null] does not match.");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node2, node1))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[null] and node2[null] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesWithValueNodesInput() throws JsonProcessingException {
    // Arrange
    JsonNode node1 = OBJECT_MAPPER.readTree("\"test\"");
    JsonNode node2 = OBJECT_MAPPER.readTree("");

    // Assert
    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[\"test\"] and node2[null] does not match.");
  }
}
