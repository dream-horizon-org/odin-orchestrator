package com.dream11.orchestrator.processor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.queue.producer.MessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamespaceMessageProcessorTest {

  @Mock MessageProducer<String> messageProducer;
  @InjectMocks NamespaceMessageProcessor namespaceMessageProcessor;

  @Mock RequestMessage nullRequestMessage;

  @Test
  void testProcessorWithNullRequestMessage() {
    assertThatThrownBy(() -> this.namespaceMessageProcessor.process(this.nullRequestMessage))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining(
            "Cannot invoke \"com.dream11.orchestrator.dto.request.NamespaceRequestMessageBody.getName()\" because \"namespaceRequestMessageBody\" is null");
  }
}
