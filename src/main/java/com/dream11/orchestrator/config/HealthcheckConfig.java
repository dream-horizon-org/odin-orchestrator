package com.dream11.orchestrator.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HealthcheckConfig {
  @NotNull Integer maxRetries = 25;
  @NotNull Integer waitSeconds = 5;
}
