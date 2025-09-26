package com.dream11.orchestrator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);
    assertEquals(1, JsonUtil.extractMatchingLeafNodes(node1, node2).size());
    assertEquals("value1", pairs.get(0).getLeft());
    assertEquals("value2", pairs.get(0).getRight());
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
    assertEquals(2, JsonUtil.extractMatchingLeafNodes(node1, node2).size());
    assertEquals("xyz.com", pairs.get(0).getLeft());
    assertEquals("pqr.com", pairs.get(0).getRight());
    assertEquals("xyz.local", pairs.get(1).getLeft());
    assertEquals("pqr.local", pairs.get(1).getRight());
  }

  @Test
  void testExtractMatchingLeafNodesThrowsError() throws JsonProcessingException {
    JsonNode node1 =
        OBJECT_MAPPER.readTree(
            "{\"discovery\":{\"public\":\"xyz.com\",\"private\":\"xyz.local\"}}");
    JsonNode node2 = OBJECT_MAPPER.readTree("{\"discovery\":{\"noMatch\":\"pqr.com\"}}");

    OrchestratorException thrown =
        assertThrows(
            OrchestratorException.class,
            () -> JsonUtil.extractMatchingLeafNodes(node1, node2),
            "Should have thrown OrchestratorException");
    assertEquals(
        "Json structure of nodes node1[public] and node2[] does not match.", thrown.getMessage());
  }
}
