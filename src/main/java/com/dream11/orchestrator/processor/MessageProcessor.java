package com.dream11.orchestrator.processor;

import com.dream11.orchestrator.dto.constants.RequestMessageType;
import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.inject.AppContext;
import lombok.NonNull;

public interface MessageProcessor {

  static MessageProcessor getProcessor(@NonNull RequestMessageType messageType) {
    if (messageType == RequestMessageType.SERVICE) {
      return AppContext.getInstance(ServiceMessageProcessor.class);
    } else if (messageType == RequestMessageType.NAMESPACE) {
      return AppContext.getInstance(NamespaceMessageProcessor.class);
    }

    throw new IllegalArgumentException("Received message type not defined");
  }

  void process(RequestMessage requestMessage);
}
