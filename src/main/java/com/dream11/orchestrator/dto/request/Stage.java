package com.dream11.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Stage {
  @NotNull Map<String, Object> config = new HashMap<>();
  @NotBlank String name;
}
