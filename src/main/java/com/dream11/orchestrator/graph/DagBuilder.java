package com.dream11.orchestrator.graph;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.GRAPH_ALREADY_INITIALIZED;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.UPDATING_VISIBLE_GRAPH_NODE_FAILED;

import com.dream11.orchestrator.dto.constants.TaskStatus;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.exception.OrchestratorException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DagBuilder {
  // graph of nodes
  private final Map<Node, List<Node>> executionGraph = new HashMap<>();
  // a measure of how many components a component depends on
  private final Map<Node, Integer> inDegrees = new HashMap<>();
  // defines the execution status of nodes
  private final Map<Node, TaskStatus> nodeStatus = new HashMap<>();
  // Map of nodes componentActionId: Node
  private final Map<Integer, Node> nodes = new HashMap<>();
  // defines if the node is available for execution or already in progress
  private final Map<Node, Boolean> visibilities = new HashMap<>();
  private boolean initialized = false;

  private void buildComponentsDag(ServiceRequestMessageBody serviceRequestMessageBody) {
    for (ComponentAction componentAction : serviceRequestMessageBody.getComponentActions()) {
      Node node = new Node(componentAction.getId(), componentAction.getName());
      this.nodes.put(componentAction.getId(), node);
      this.executionGraph.put(node, new ArrayList<>());
      this.inDegrees.put(node, 0);
      this.visibilities.put(node, true);
      this.nodeStatus.put(node, TaskStatus.CREATED);
    }
    for (ComponentAction componentAction : serviceRequestMessageBody.getComponentActions()) {
      Node node = this.nodes.get(componentAction.getId());
      if (componentAction.hasDependsOn()) {
        for (Integer dependentComponentActionId : componentAction.getDependsOn()) {
          Node upstreamNode = this.nodes.get(dependentComponentActionId);
          addDependentNode(upstreamNode, node);
          incrementInDegree(node);
        }
      }
    }
  }

  public boolean isEmpty() {
    return this.executionGraph.isEmpty();
  }

  public void reset() {
    this.executionGraph.clear();
  }

  /*
   * Clear all nodes connected to the given componentActionId
   */
  public void clearConnectedComponents(Integer componentActionId) {
    checkInit();
    List<Node> visited = getConnectedNodes(componentActionId);
    // remove all visited nodes from the graph
    visited.forEach(this.executionGraph::remove);
    // disable visibility of all visited nodes
    visited.forEach(this::setVisibilityFalse);
    // set node status to FAILED
    visited.forEach(node1 -> this.nodeStatus.put(node1, TaskStatus.FAILED));
  }

  private List<Node> getConnectedNodes(Integer componentActionId) {
    checkInit();
    Node node = this.nodes.get(componentActionId);
    Set<Node> visited = new HashSet<>();
    Deque<Node> stack = new ArrayDeque<>();

    stack.push(node);
    visited.add(node);

    while (!stack.isEmpty()) {
      Node currentNode = stack.pop();
      for (Node neighbor : executionGraph.get(currentNode)) {
        if (!visited.contains(neighbor) && Boolean.TRUE.equals(visibilities.get(neighbor))) {
          stack.push(neighbor);
          visited.add(neighbor);
        }
      }
    }
    return visited.stream().toList();
  }

  public List<Integer> getDependentComponentActionIds(Integer componentActionId) {
    return getConnectedNodes(componentActionId).stream()
        .map(Node::componentActionId)
        .filter(id -> !id.equals(componentActionId))
        .toList();
  }

  public void init(ServiceRequestMessageBody serviceRequestMessageBody) {
    if (this.initialized) {
      throw new OrchestratorException(GRAPH_ALREADY_INITIALIZED);
    }
    this.initialized = true;
    buildComponentsDag(serviceRequestMessageBody);
  }

  public Set<Integer> getNextComponentActionIds() {
    checkInit();
    return this.nodes.values().stream()
        .map(
            node -> {
              if (this.inDegrees.get(node).equals(0)
                  && Boolean.TRUE.equals(this.visibilities.get(node))) {
                setVisibilityFalse(node);
                this.nodeStatus.put(node, TaskStatus.IN_PROGRESS);
                return node.componentActionId();
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public void updateCompletedNode(Integer componentActionId, TaskStatus status) {
    checkInit();
    Node node = this.nodes.get(componentActionId);

    if (Boolean.TRUE.equals(visibilities.get(node))) {
      throw new OrchestratorException(UPDATING_VISIBLE_GRAPH_NODE_FAILED, node.componentName);
    }
    if (this.executionGraph.get(node) != null) {
      this.executionGraph.remove(node).forEach(this::decrementInDegree);
    }
    this.nodeStatus.put(node, status);
  }

  private void incrementInDegree(Node node) {
    this.inDegrees.put(node, this.inDegrees.get(node) + 1);
  }

  private void decrementInDegree(Node node) {
    this.inDegrees.put(node, this.inDegrees.get(node) - 1);
  }

  private void setVisibilityFalse(Node node) {
    this.visibilities.put(node, Boolean.FALSE);
  }

  public void setVisibilityFalse(Integer componentActionId) {
    Node node = this.nodes.get(componentActionId);
    setVisibilityFalse(node);
  }

  private void addDependentNode(Node node, Node dependentNode) {
    this.executionGraph.computeIfAbsent(node, k -> new ArrayList<>()).add(dependentNode);
  }

  private void checkInit() {
    if (!this.initialized) {
      throw new IllegalStateException("Graph not initialized. Use init() first");
    }
  }

  public boolean executionSuccess() {
    return this.nodeStatus.values().stream().allMatch(value -> value.equals(TaskStatus.SUCCESSFUL));
  }

  public boolean executionFailure() {
    return this.nodeStatus.values().stream().anyMatch(value -> value.equals(TaskStatus.FAILED));
  }

  record Node(Integer componentActionId, String componentName) {}
}
