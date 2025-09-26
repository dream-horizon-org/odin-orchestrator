package com.dream11.orchestrator.dto.account.servicedata;

import com.dream11.orchestrator.constants.NamespaceProviderType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class K8sServiceData {
  @NotNull List<@Valid Cluster> clusters = new ArrayList<>();

  @Data
  public static class Cluster {
    @NotBlank String name;
    @NotBlank String kubeconfig; // Base64 encoded kube config
    @NotNull @Valid NamespaceConfig namespaceConfig;
  }

  @Data
  public static class NamespaceConfig {
    @NotNull @Valid NamespaceProvider provider;
  }

  @Data
  public static class NamespaceProvider {
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "name")
    @JsonSubTypes({@JsonSubTypes.Type(value = OdinNamespaceProviderConfig.class, name = "ODIN")})
    @NotNull
    @Valid
    NamespaceProviderConfig config;

    @NotNull NamespaceProviderType name;
  }
}
