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
    JsonNode node1 = OBJECT_MAPPER.readTree("{\"key1\":\"value1\"}");
    JsonNode node2 = OBJECT_MAPPER.readTree("{\"key1\":\"value2\"}");
    JsonNode node3 = OBJECT_MAPPER.readTree("");
    List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);
    assertThat(1).isEqualTo(JsonUtil.extractMatchingLeafNodes(node1, node2).size());
    assertThat("value1").isEqualTo(pairs.get(0).getLeft());
    assertThat("value2").isEqualTo(pairs.get(0).getRight());

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node2, node3))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[{\"key1\":\"value2\"}] and node2[null] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesNestedJson() throws JsonProcessingException {
    JsonNode node1 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"xyz.com\",\"private\":\"xyz.local\"}}");
    JsonNode node2 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"pqr.com\",\"private\":\"pqr.local\"}}");
    List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);
    assertThat(2).isEqualTo(JsonUtil.extractMatchingLeafNodes(node1, node2).size());
    assertThat("xyz.com").isEqualTo(pairs.get(0).getLeft());
    assertThat("pqr.com").isEqualTo(pairs.get(0).getRight());
    assertThat("xyz.local").isEqualTo(pairs.get(1).getLeft());
    assertThat("pqr.local").isEqualTo(pairs.get(1).getRight());
  }

  @Test
  void testExtractMatchingLeafNodesDifferentTrees() throws JsonProcessingException {
    JsonNode node1 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"xyz.com\",\"private\":\"xyz.local\"}}");
    JsonNode node2 = OBJECT_MAPPER.readTree("{\"discovery\":{\"noMatch\":\"pqr.com\"}}");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Json structure of nodes node1[public] and node2[] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesArrayInput() throws JsonProcessingException {
    JsonNode node1 = OBJECT_MAPPER.readTree("[\"key\"]");

    assertThat(1).isEqualTo(JsonUtil.extractMatchingLeafNodes(node1, node1).size());

    JsonNode node3 = OBJECT_MAPPER.readTree("[\"key\", \"value1\"]");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node3))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\"]] and node2[[\"key\",\"value1\"]] does not match.");

    JsonNode node4 = OBJECT_MAPPER.readTree("[\"key\", \"value2\"]");
    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node3, node4))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\",\"value1\"]] and node2[[\"key\",\"value2\"]] does not match.");

    JsonNode objectNode = OBJECT_MAPPER.readTree("{\"discovery\":{\"public\":\"xyz.com\"}}");
    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, objectNode))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[[\"key\"]] and node2[{\"discovery\":{\"public\":\"xyz.com\"}}] does not match.");
  }

  @Test
  void testExtractMatchingLeafNodesNullInput() throws JsonProcessingException {
    JsonNode node1 = null;
    JsonNode node2 = OBJECT_MAPPER.readTree("");

    List<Pair<String, String>> NullPairs = JsonUtil.extractMatchingLeafNodes(node1, node1);
    assertThat(0).isEqualTo(NullPairs.size());

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
    JsonNode node1 = OBJECT_MAPPER.readTree("\"test\"");
    JsonNode node2 = OBJECT_MAPPER.readTree("");

    assertThatThrownBy(() -> JsonUtil.extractMatchingLeafNodes(node1, node2))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(
            "Json structure of nodes node1[\"test\"] and node2[null] does not match.");
  }
}
