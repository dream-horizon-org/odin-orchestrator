package com.dream11.orchestrator.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
  @JsonProperty("data")
  @NotNull
  @Builder.Default
  Map<String, Object> accountData = new HashMap<>();

  @NotBlank String category;
  @NotBlank String name;
  @NotBlank String provider;
  @NotNull @Builder.Default List<@Valid Service> services = new ArrayList<>();
}
