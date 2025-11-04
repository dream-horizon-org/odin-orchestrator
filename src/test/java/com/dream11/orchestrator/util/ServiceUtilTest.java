package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ServiceUtilTest {

  @Test
  void testGetComponentActionByIdWithEmptyInput() {
    assertThatThrownBy(() -> ServiceUtil.getComponentActionById(0, List.of()))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Component action not found for actionId : 0");
  }

  @Test
  void testGetComponentActionByIdTrueCase() {
    ComponentAction componentAction = new ComponentAction();
    componentAction.setId(0);

    assertThat(componentAction)
        .isEqualTo(ServiceUtil.getComponentActionById(0, List.of(componentAction)));
  }
}
