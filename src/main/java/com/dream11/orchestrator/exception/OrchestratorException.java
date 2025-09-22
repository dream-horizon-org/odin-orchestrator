package com.dream11.orchestrator.exception;

public class OrchestratorException extends RuntimeException {

  public OrchestratorException(
      OrchestratorExceptionType orchestratorExceptionType, Object... params) {
    super(orchestratorExceptionType.formatMessage(params));
  }
}
