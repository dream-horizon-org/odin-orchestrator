package com.dream11.orchestrator.util;

import com.dream11.orchestrator.constants.RequestMessageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RunnerUtilsTest {

  @Test
  void testGetRunnerNamespace() {
    Assertions.assertEquals(
        "odin-service-123",
        RunnerUtils.getRunnerNamespace(RequestMessageType.SERVICE.getName(), 123));
  }
}
