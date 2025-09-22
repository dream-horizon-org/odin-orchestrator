package com.dream11.orchestrator.config.dsl;

import java.util.Map;

public interface StateConfig {
  Map<String, String> getConfig(String path);
}
