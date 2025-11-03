package com.dream11.orchestrator.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.exception.OrchestratorException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServiceUtilTest {

  @Test
  void testGetComponentActionByIdWithEmptyInput() {
    assertThrows(
        OrchestratorException.class,
        () -> ServiceUtil.getComponentActionById(0, List.of()),
        "Should have thrown OrchestratorException");
  }
}
