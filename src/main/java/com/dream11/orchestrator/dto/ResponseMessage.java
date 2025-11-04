package com.dream11.orchestrator.dto;

import com.dream11.orchestrator.constants.ResponseMessageType;
import com.dream11.orchestrator.constants.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ResponseMessage {
  String error;

  String executionId;

  Long id;

  TaskStatus status;

  ResponseMessageType type;

  ResponseData data;
}
