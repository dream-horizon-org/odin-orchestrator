package com.dream11.orchestrator.dto.account.servicedata;

import com.dream11.orchestrator.constants.NamespaceProviderType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class K8sServiceData {
  List<Cluster> clusters = new ArrayList<>();

  String serviceIAMRole;

  String pullSecrets;

  Map<String, String> serviceAnnotations = new HashMap<>();

  Map<String, String> serviceAccountAnnotations = new HashMap<>();

  Map<String, String> environmentVariables = new HashMap<>();

  @Data
  public static class Cluster {
    String name;
    String kubeconfig; // Base64 encoded kube config
    NamespaceConfig namespaceConfig;
    Map<String, List<String>> labels = new HashMap<>();
  }

  @Data
  public static class NamespaceConfig {
    NamespaceProvider provider;
  }

  @Data
  public static class NamespaceProvider {
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "name")
    @JsonSubTypes({@JsonSubTypes.Type(value = OdinNamespaceProviderConfig.class, name = "ODIN")})
    NamespaceProviderConfig config;

    NamespaceProviderType name;
  }
}
