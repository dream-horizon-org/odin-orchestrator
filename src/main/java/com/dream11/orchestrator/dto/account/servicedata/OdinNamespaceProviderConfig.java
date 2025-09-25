package com.dream11.orchestrator.dto.account.servicedata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class OdinNamespaceProviderConfig implements NamespaceProviderConfig {
  @NotNull Map<String, String> annotations = new HashMap<>();
  @NotNull List<@Valid WorkLoad> workloads = new ArrayList<>();

  @Data
  public static class WorkLoad {
    @NotBlank String repo;
    @NotNull String username = "";
    @NotNull String password = "";
    @NotBlank String chart;
    @NotBlank String version;
    @NotNull Map<String, Object> values = new HashMap<>();
  }
}
