package com.dream11.orchestrator.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NamespaceResponseData implements ResponseData {
  String accountName;
}
