package com.dream11.orchestrator.dto;

import com.dream11.orchestrator.dto.request.ComponentAction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestServiceDto {
  ComponentAction componentAction;
  Long deploymentId;
  String environmentName;
  String serviceName;
  long orgId;
  String deploymentNamespace;
}
