package com.dream11.orchestrator.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public enum OrchestratorExceptionType {
  APP_CONTEXT_UNINITIALIZED("OE01", "Application context not initialized"),
  APP_CONTEXT_ALREADY_INITIALIZED("OE02", "Application context is already initialized"),
  EXECUTOR_SERVICE_UNINITIALIZED(
      "OE06", "Deployer service object uninitialized. Call init() first"),
  GRAPH_ALREADY_INITIALIZED("OE07", "Graph already initialized"),
  INVALID_ACCOUNT_OBJECT("OE08", "Invalid account object"),
  INVALID_COMPONENT_ACTION_ID("OE09", "Component action not found for actionId : %d"),
  INVALID_COMPONENTS_DATA("OE10", "Invalid component actions list or components list"),
  MANIFEST_SERVICE_UNINITIALIZED(
      "OE11", "Manifest service object uninitialized. Call init() first"),
  NAMESPACE_CREATION_FAILED("OE12", "Namespace creation failed for %s with error %s"),
  UPDATING_VISIBLE_GRAPH_NODE_FAILED("OE16", "Cannot update visible graph node %s"),

  DISCOVERY_OUTPUT_NOT_FOUND_IN_LOGS("OE17", "No discovery output found in logs"),

  DISCOVERY_OUTPUT_DOES_NOT_MATCH_INPUT("OE19", "Discovery output doesn't match with input"),

  INVALID_DISCOVERY_ACTION("OE20", "Invalid discovery action %s"),

  INVALID_JSON_STRUCTURE("OE21", "Json structure of nodes node1[%s] and node2[%s] does not match."),
  HELM_COMMAND_EXECUTION_FAILED("OE23", "Failed to execute helm command, error:[%s]"),
  RUNNER_DISCOVERY_OUTPUT_NOT_FOUND("OE24", "Runner discovery output not found in logs"),
  DISCOVERY_SERVICE_ERROR("OE25", "Operation completed ,discovery update failed due to error: %s"),
  WAITER_INTERRUPTED("OE26", "Thread interrupted while waiting for namespace creation : %s");
  private final String errorCode;
  private final String errorMessage;

  public String formatMessage(Object... params) {
    return String.format(this.errorMessage, params);
  }
}
