package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.exception.OrchestratorExceptionType;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;

public class ServiceUtilsTest {
  final ObjectMapper objectMapper = AppContext.getObjectMapper();

  @Test
  @SneakyThrows
  public void testValidateComponentsFailIfComponentNameDoesNotExist() {
    // Arrange
    ComponentAction componentAction =
        this.objectMapper.readValue(
            TestUtil.getComponentAction("comp1", "validate", "{}").toString(),
            ComponentAction.class);
    componentAction.setName(null);

    // Act & Assert
    assertThatThrownBy(() -> ServiceUtils.validateComponents(List.of(componentAction)))
        .isInstanceOf(OrchestratorException.class)
        .hasMessage(OrchestratorExceptionType.INVALID_COMPONENTS_DATA.getErrorMessage());
  }

  @Test
  @SneakyThrows
  public void testValidateComponentsFailIfComponentNameIsEmpty() {
    // Arrange
    ComponentAction componentAction =
        this.objectMapper.readValue(
            TestUtil.getComponentAction("comp1", "validate", "{}").toString(),
            ComponentAction.class);
    componentAction.setName("");

    // Act & Assert
    assertThatThrownBy(() -> ServiceUtils.validateComponents(List.of(componentAction)))
        .isInstanceOf(OrchestratorException.class)
        .hasMessage(OrchestratorExceptionType.INVALID_COMPONENTS_DATA.getErrorMessage());
  }

  @Test
  @SneakyThrows
  public void testValidateComponentsFailIfComponentActionIdDoesNotExist() {
    // Arrange
    ComponentAction componentAction =
        this.objectMapper.readValue(
            TestUtil.getComponentAction("comp1", "validate", "{}").toString(),
            ComponentAction.class);
    componentAction.setId(null);

    // Act & Assert
    assertThatThrownBy(() -> ServiceUtils.validateComponents(List.of(componentAction)))
        .isInstanceOf(OrchestratorException.class)
        .hasMessage(OrchestratorExceptionType.INVALID_COMPONENTS_DATA.getErrorMessage());
  }
}
