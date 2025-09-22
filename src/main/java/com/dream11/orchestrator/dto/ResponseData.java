package com.dream11.orchestrator.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResponseData {
  String componentName;
  String stage;
}
