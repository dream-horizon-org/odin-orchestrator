package com.dream11.orchestrator.processor;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.queue.producer.MessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NamespaceMessageProcessorTest {

  @Mock MessageProducer<String> messageProducer;
  @InjectMocks NamespaceMessageProcessor namespaceMessageProcessor;

  @Mock RequestMessage nullRequestMessage;

  @Test
  void testProcessorWithNullRequestMessage() {
    assertThrows(
        NullPointerException.class,
        () -> this.namespaceMessageProcessor.process(this.nullRequestMessage),
        "Should have thrown NullPointerException");

    this.namespaceMessageProcessor.toString();
  }
}
