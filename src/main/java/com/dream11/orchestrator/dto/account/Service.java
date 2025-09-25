package com.dream11.orchestrator.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service {
  @NotBlank String name;
  @NotBlank String category;
  @NotNull Map<String, Object> data = new HashMap<>();
}
