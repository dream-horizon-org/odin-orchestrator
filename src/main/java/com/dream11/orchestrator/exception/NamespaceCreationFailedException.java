package com.dream11.orchestrator.exception;

import java.io.IOException;

public class NamespaceCreationFailedException extends IOException {

  public NamespaceCreationFailedException(String message) {
    super(message);
  }
}
