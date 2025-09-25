package com.dream11.orchestrator.dto.request;

import com.dream11.orchestrator.dto.account.AccountDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ComponentAction {

  @NotNull Map<String, Object> baseConfig = new HashMap<>();
  @NotNull Map<String, Object> flavourConfig = new HashMap<>();
  @NotNull Map<String, Object> operationConfig = new HashMap<>();
  @NotBlank String name;
  @NotBlank String type;
  @NotBlank String version;
  @NotNull List<Integer> dependsOn = new ArrayList<>();
  @NotBlank String deploymentType;
  @NotNull Integer id;
  @NotNull @Valid Stage stage;
  @NotNull String provider;
  @NotNull @Valid AccountDto accounts = new AccountDto();

  public boolean hasDependsOn() {
    return this.dependsOn != null && !this.dependsOn.isEmpty();
  }
}
