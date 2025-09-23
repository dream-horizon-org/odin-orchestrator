package com.dream11.orchestrator.dto.discovery;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoveryRecord {
  String name;
  List<String> values;
}
