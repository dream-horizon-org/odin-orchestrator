package com.dream11.orchestrator.config;

import com.fasterxml.jackson.annotation.JsonProperty;
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

  @Data
  public static class DindConfig {
    @NotBlank String image;
    @NotBlank String imagePullPolicy = "Always";
    @NotNull Boolean enabled = true;
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
  }
}
