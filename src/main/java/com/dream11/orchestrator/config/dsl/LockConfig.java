package com.dream11.orchestrator.config.dsl;

import java.util.Map;

public interface LockConfig {
  Map<String, Object> getConfig(String key);
}
