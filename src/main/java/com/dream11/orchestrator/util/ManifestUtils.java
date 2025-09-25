package com.dream11.orchestrator.util;

import com.dream11.orchestrator.dto.metadata.CloudProviderDetails;
import com.dream11.orchestrator.dto.metadata.ComponentMetaData;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.inject.AppContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ManifestUtils {
  public String getSecretName(String componentName, int componentExecutionId) {
    return String.format("%s-%d-secret", componentName, componentExecutionId);
  }

  public String getConfigMapName(String componentName, int componentExecutionId) {
    return String.format("%s-%d-configmap", componentName, componentExecutionId);
  }

  public String getJobName(String componentName, int componentExecutionId) {
    return String.format("%s-%d-job", componentName, componentExecutionId);
  }

  public String getServiceAccountName(String componentName, int componentExecutionId) {
    return String.format("%s-service-%d-account", componentName, componentExecutionId);
  }

  public String getNamespace(String envName, String serviceName, Long deploymentId) {
    // Append traceId to name (if available)
    String traceIdSuffix =
        AppContext.getTraceId().isEmpty() ? "" : "-" + AppContext.getTraceId().split("-")[0];
    return String.format("%s-%s-%d%s", envName, serviceName, deploymentId, traceIdSuffix);
  }

  public ComponentMetaData buildComponentMetaData(
      ComponentAction componentAction,
      String envName,
      String deploymentNamespace,
      long orgId,
      Long deploymentId) {
    return ComponentMetaData.builder()
        .cloudProviderDetails(
            CloudProviderDetails.builder()
                .account(componentAction.getAccounts().getAccount())
                .linkedAccounts(componentAction.getAccounts().getLinkedAccounts())
                .build())
        .name(componentAction.getName())
        .envName(envName)
        .deploymentNamespace(deploymentNamespace)
        .operationId(String.valueOf(deploymentId))
        .orgId(orgId)
        .build();
  }
}
