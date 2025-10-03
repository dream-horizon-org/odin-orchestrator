package com.dream11.orchestrator.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Quantity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RunnerConfig {
  @NotBlank String image;
  @NotBlank String imagePullPolicy = "Always";

  @JsonProperty("env")
  @NotNull
  Map<String, String> envVars = new HashMap<>();

  @NotNull @Valid DindConfig dind = new DindConfig();

  @NotNull List<@Valid DockerSecret> dockerSecrets = new ArrayList<>();

  @NotNull List<@Valid HostVolumeMounts> hostVolumeMounts = new ArrayList<>();
  @Valid @NotNull Resources resources = new Resources();

  @Data
  public static class DindConfig {
    @NotBlank String image;
    @NotBlank String imagePullPolicy = "Always";
    @NotNull Boolean enabled = true;
    @Valid @NotNull Resources resources = new Resources();
  }

  @Data
  public static class DockerSecret {
    @NotBlank String name;
    @NotBlank String server;
    @NotBlank String username;
    @NotBlank String password;
  }

  @Data
  public static class HostVolumeMounts {
    @NotBlank String name;
    @NotBlank String hostPath;
    @NotBlank String mountPath;
    @NotBlank String type = "Directory";
  }

  @Data
  public static class Resources {
    @NotNull Map<String, Quantity> requests = new HashMap<>();
    @NotNull Map<String, Quantity> limits = new HashMap<>();
  }
}
