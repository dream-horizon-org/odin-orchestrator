package com.dream11.orchestrator.util;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.INVALID_COMPONENTS_DATA;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.INVALID_COMPONENT_ACTION_ID;

import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceUtils {

  public ComponentAction getComponentActionById(
      Integer componentActionId, List<ComponentAction> componentActions) {
    return componentActions.stream()
        .filter(componentAction -> componentAction.getId().equals(componentActionId))
        .findFirst()
        .orElseThrow(
            () -> new OrchestratorException(INVALID_COMPONENT_ACTION_ID, componentActionId));
  }

  public void validateComponents(List<ComponentAction> componentActions) {
    if (componentActions.stream()
        .anyMatch(
            componentAction ->
                componentAction.getId() == null
                    || componentAction.getName() == null
                    || componentAction.getName().isEmpty())) {
      throw new OrchestratorException(INVALID_COMPONENTS_DATA);
    }
  }
}
