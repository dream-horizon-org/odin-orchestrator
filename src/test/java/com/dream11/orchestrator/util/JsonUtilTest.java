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
        JsonNode node3 = OBJECT_MAPPER.readTree("");
        List<Pair<String, String>> pairs = JsonUtil.extractMatchingLeafNodes(node1, node2);
        assertEquals(1, JsonUtil.extractMatchingLeafNodes(node1, node2).size());
        assertEquals("value1", pairs.get(0).getLeft());
        assertEquals("value2", pairs.get(0).getRight());

        OrchestratorException thrown =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node2, node3),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[{\"key1\":\"value2\"}] and node2[null] does not match.", thrown.getMessage());
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
    void testExtractMatchingLeafNodesDifferentTrees() throws JsonProcessingException {
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

    @Test
    void testExtractMatchingLeafNodesArrayInput() throws JsonProcessingException {
        JsonNode node1 = OBJECT_MAPPER.readTree("[\"key\"]");

        assertEquals(1, JsonUtil.extractMatchingLeafNodes(node1, node1).size());

        JsonNode node3 = OBJECT_MAPPER.readTree("[\"key\", \"value1\"]");

        OrchestratorException thrown1 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node1, node3),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[[\"key\"]] and node2[[\"key\",\"value1\"]] does not match.", thrown1.getMessage());

        JsonNode node4 = OBJECT_MAPPER.readTree("[\"key\", \"value2\"]");
        OrchestratorException thrown2 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node3, node4),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[[\"key\",\"value1\"]] and node2[[\"key\",\"value2\"]] does not match.", thrown2.getMessage());

        JsonNode objectNode = OBJECT_MAPPER.readTree("{\"discovery\":{\"public\":\"xyz.com\"}}");
        OrchestratorException thrown3 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node1, objectNode),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[[\"key\"]] and node2[{\"discovery\":{\"public\":\"xyz.com\"}}] does not match.", thrown3.getMessage());
    }

    @Test
    void testExtractMatchingLeafNodesNullInput() throws JsonProcessingException {
        JsonNode node1 = null;
        JsonNode node2 = OBJECT_MAPPER.readTree("");

        List<Pair<String, String>> NullPairs = JsonUtil.extractMatchingLeafNodes(node1, node1);
        assertEquals(0, NullPairs.size());

        OrchestratorException thrown1 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node1, node2),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[null] and node2[null] does not match.", thrown1.getMessage());

        OrchestratorException thrown2 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node2, node1),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[null] and node2[null] does not match.", thrown2.getMessage());
    }

    @Test
    void testExtractMatchingLeafNodesWithValueNodesInput() throws JsonProcessingException {
        JsonNode node1 = OBJECT_MAPPER.readTree("\"test\"");
        JsonNode node2 = OBJECT_MAPPER.readTree("");

        OrchestratorException thrown1 =
                assertThrows(
                        OrchestratorException.class,
                        () -> JsonUtil.extractMatchingLeafNodes(node1, node2),
                        "Should have thrown OrchestratorException");
        assertEquals(
                "Json structure of nodes node1[\"test\"] and node2[null] does not match.", thrown1.getMessage());
    }
}
