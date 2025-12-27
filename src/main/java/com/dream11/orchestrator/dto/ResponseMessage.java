package com.dream11.orchestrator.dto;

import com.dream11.orchestrator.constants.ResponseMessageType;
import com.dream11.orchestrator.constants.TaskStatus;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class ResponseMessage {
  String error;
  Long id;
  TaskStatus status;
  ResponseMessageType type;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = NamespaceResponseData.class, name = "NAMESPACE"),
    @JsonSubTypes.Type(value = ServiceResponseData.class, name = "COMPONENT_STATUS"),
    @JsonSubTypes.Type(value = ServiceResponseData.class, name = "SERVICE_STATUS")
  })
  ResponseData data;

  String executionId;
}
