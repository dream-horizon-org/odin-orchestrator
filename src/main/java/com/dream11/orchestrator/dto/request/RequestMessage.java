package com.dream11.orchestrator.dto.request;

import com.dream11.orchestrator.dto.constants.RequestMessageType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestMessage {
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceRequestMessageBody.class, name = "SERVICE"),
    @JsonSubTypes.Type(value = NamespaceRequestMessageBody.class, name = "NAMESPACE")
  })
  RequestMessageBody body;

  Long id;
  RequestMessageType type;
  String traceId;
}
