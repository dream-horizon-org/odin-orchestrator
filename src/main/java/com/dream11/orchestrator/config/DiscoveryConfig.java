package com.dream11.orchestrator.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiscoveryConfig {
  @NotNull Integer maxRetries = 3;
  @NotNull Integer retryIntervalSecs = 2;
  @NotBlank String url;
  @NotNull Integer timeoutSecs = 10;
}
