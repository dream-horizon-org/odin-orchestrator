package com.dream11.orchestrator.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRequestMessageBody implements RequestMessageBody {
  @NotNull List<@Valid ComponentAction> componentActions;

  @NotBlank String environmentName;

  @NotBlank String serviceName;
  long orgId;
}
