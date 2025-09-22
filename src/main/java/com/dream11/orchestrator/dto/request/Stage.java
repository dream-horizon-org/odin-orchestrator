package com.dream11.orchestrator.dto.request;

import java.util.Map;
import lombok.Data;

@Data
public class Stage {
  Map<String, Object> config;
  String name;
}
