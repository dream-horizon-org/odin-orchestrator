package com.dream11.orchestrator.dto.metadata;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DslMetaData {
  Map<String, Object> config;
  String flavour;
  String stage;
  Map<String, Object> stateConfig;
  Map<String, Object> lockConfig;
}
