package com.dream11.orchestrator.dto.metadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComponentMetaData {

  CloudProviderDetails cloudProviderDetails;
  String envName;
  String name;
  String deploymentNamespace;
  long orgId;
  String operationId;
}
