package com.dream11.orchestrator.dto.metadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComponentUserData {
  String postServiceStart;
  String preServiceStart;
}
