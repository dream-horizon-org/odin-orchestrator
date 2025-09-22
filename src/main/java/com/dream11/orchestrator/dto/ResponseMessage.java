package com.dream11.orchestrator.dto;

import com.dream11.orchestrator.dto.constants.ResponseMessageType;
import com.dream11.orchestrator.dto.constants.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ResponseMessage {
  String error;

  Long id;

  TaskStatus status;

  ResponseMessageType type;

  ResponseData data;
}
