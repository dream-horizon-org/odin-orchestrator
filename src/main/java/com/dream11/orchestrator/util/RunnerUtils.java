package com.dream11.orchestrator.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RunnerUtils {
  public String getRunnerNamespace(String messageType, int id) {
    return String.format("odin-%s-%s", messageType, id);
  }
}
