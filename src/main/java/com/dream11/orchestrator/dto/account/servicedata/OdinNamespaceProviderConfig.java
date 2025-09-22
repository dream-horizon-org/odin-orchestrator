package com.dream11.orchestrator.dto.account.servicedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OdinNamespaceProviderConfig implements NamespaceProviderConfig {
  Map<String, String> annotations = new HashMap<>();
  List<WorkLoad> workloads = new ArrayList<>();

  @Data
  public static class WorkLoad {
    String repo;
    String username;
    String password;
    String chart;
    String version;
    Map<String, Object> values;
  }
}
