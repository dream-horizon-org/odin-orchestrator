package com.dream11.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("squid:S2094")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestMessageBody implements RequestMessageBody {
  List<ComponentAction> componentActions;

  @JsonProperty("environmentName")
  String envName;

  String serviceName;
  long orgId;
}
