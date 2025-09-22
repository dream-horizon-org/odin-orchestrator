package com.dream11.orchestrator.config;

import com.dream11.orchestrator.config.dsl.DslConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
  @NotNull @Valid QueueConfig queue;
  @NotNull @Valid ComponentRegistryConfig componentRegistry;
  @NotNull @Valid DslConfig dsl;
  @NotNull @Valid RunnerConfig runner;
  @NotNull @Valid HealthcheckConfig healthcheck;
  @NotNull @Valid DiscoveryConfig discovery;
}
