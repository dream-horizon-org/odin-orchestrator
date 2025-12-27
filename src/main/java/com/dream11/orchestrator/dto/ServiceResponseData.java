package com.dream11.orchestrator.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ServiceResponseData implements ResponseData {
  String componentName;
  String stage;
}
