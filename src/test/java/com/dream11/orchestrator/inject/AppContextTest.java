package com.dream11.orchestrator.inject;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.APP_CONTEXT_ALREADY_INITIALIZED;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.APP_CONTEXT_UNINITIALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.dream11.orchestrator.Orchestrator;
import com.dream11.orchestrator.exception.OrchestratorException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppContextTest {

  @BeforeAll
  static void setup() {
    AppContext.reset();
    AppContext.initialize(List.of(new MainModule()));
  }

  @Test
  void testMultipleInstance() {
    assertThatThrownBy(() -> AppContext.initialize(List.of(new MainModule())))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(APP_CONTEXT_ALREADY_INITIALIZED.getErrorMessage());
  }

  @Test
  void testNullInstance() {
    AppContext.reset();
    assertThatThrownBy(() -> AppContext.getInstance(Orchestrator.class))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining(APP_CONTEXT_UNINITIALIZED.getErrorMessage());
  }

  @Test
  void testSetTraceId() {
    String traceId = "traceId";
    AppContext.setTraceId(traceId);
    assertThat(traceId).isEqualTo(AppContext.getTraceId());
  }
}
