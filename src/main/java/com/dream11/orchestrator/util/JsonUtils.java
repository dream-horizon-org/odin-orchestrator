package com.dream11.orchestrator.util;

import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.exception.OrchestratorExceptionType;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = AppContext.getObjectMapper();

  @SneakyThrows
  public <T> String toJsonString(T obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  @SneakyThrows
  public List<Pair<String, String>> extractMatchingLeafNodes(JsonNode node1, JsonNode node2) {
    return extractLeafNodesRecursive(node1, node2);
  }

  @SneakyThrows
  private List<Pair<String, String>> extractLeafNodesRecursive(JsonNode node1, JsonNode node2) {

    List<Pair<String, String>> leafPairs = new ArrayList<>();

    if (node1 == null && node2 == null) {
      return leafPairs;
    }
    if (node1 == null || node2 == null) {
      throw new OrchestratorException(
          OrchestratorExceptionType.INVALID_JSON_STRUCTURE,
          OBJECT_MAPPER.writeValueAsString(node1),
          OBJECT_MAPPER.writeValueAsString(node2));
    }

    if (node1.isObject() && node2.isObject()) {
      for (Iterator<String> it = node1.fieldNames(); it.hasNext(); ) {
        String field = it.next();
        if (node2.has(field)) {
          leafPairs.addAll(extractLeafNodesRecursive(node1.get(field), node2.get(field)));
        } else {
          throw new OrchestratorException(
              OrchestratorExceptionType.INVALID_JSON_STRUCTURE, field, "");
        }
      }
    } else if (node1.isArray() && node2.isArray()) {
      // Ensure both arrays have the same size
      if (node1.size() != node2.size()) {
        throw new OrchestratorException(
            OrchestratorExceptionType.INVALID_JSON_STRUCTURE,
            OBJECT_MAPPER.writeValueAsString(node1),
            OBJECT_MAPPER.writeValueAsString(node2));
      }

      // Convert arrays to lists of JSON strings and sort them
      List<String> sortedNode1 = new ArrayList<>();
      List<String> sortedNode2 = new ArrayList<>();
      for (JsonNode element : node1) {
        sortedNode1.add(OBJECT_MAPPER.writeValueAsString(element));
      }
      for (JsonNode element : node2) {
        sortedNode2.add(OBJECT_MAPPER.writeValueAsString(element));
      }

      Collections.sort(sortedNode1);
      Collections.sort(sortedNode2);

      // Ensure the sorted lists are equal
      if (!sortedNode1.equals(sortedNode2)) {
        throw new OrchestratorException(
            OrchestratorExceptionType.INVALID_JSON_STRUCTURE,
            OBJECT_MAPPER.writeValueAsString(node1),
            OBJECT_MAPPER.writeValueAsString(node2));
      }

      // Recursively compare elements in sorted order
      for (int i = 0; i < sortedNode1.size(); i++) {
        JsonNode sortedNode1Element = OBJECT_MAPPER.readTree(sortedNode1.get(i));
        JsonNode sortedNode2Element = OBJECT_MAPPER.readTree(sortedNode2.get(i));
        leafPairs.addAll(extractLeafNodesRecursive(sortedNode1Element, sortedNode2Element));
      }
    } else {
      // Check if both nodes are leaf nodes
      if (node1.isValueNode() && node2.isValueNode()) {
        leafPairs.add(Pair.of(node1.asText(), node2.asText()));
      } else {
        throw new OrchestratorException(
            OrchestratorExceptionType.INVALID_JSON_STRUCTURE,
            OBJECT_MAPPER.writeValueAsString(node1),
            OBJECT_MAPPER.writeValueAsString(node2));
      }
    }

    return leafPairs;
  }
}
