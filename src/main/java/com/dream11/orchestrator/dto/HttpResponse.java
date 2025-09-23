package com.dream11.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class HttpResponse {
  int statusCode;
  String message;
  String body;
}
