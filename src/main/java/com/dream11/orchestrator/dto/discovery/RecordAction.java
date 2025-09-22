package com.dream11.orchestrator.dto.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordAction {
  String action;
  String id;

  @JsonProperty("record")
  DiscoveryRecord discoveryRecord;
}
