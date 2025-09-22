package com.dream11.orchestrator.config;

import lombok.Data;

@Data
public class HttpClientConfig {
  Integer maxRetries;
  Integer retryIntervalSecs;
  Integer timeoutSecs;
}
