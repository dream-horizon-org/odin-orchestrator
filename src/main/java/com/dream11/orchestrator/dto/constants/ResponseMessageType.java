package com.dream11.orchestrator.dto.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ResponseMessageType {
  NAMESPACE("NAMESPACE"),
  COMPONENT_STATUS("COMPONENT_STATUS"),
  SERVICE_STATUS("SERVICE_STATUS");
  private final String name;
}
