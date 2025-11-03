package com.dream11.orchestrator.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.constants.RequestMessageType;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.util.ConfigUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class MessageProcessorTest {

  @Mock RequestMessageType nullRequestMessageType;

  @BeforeAll
  static void setup() {
    AppContext.initialize(
        List.of(new MainModule(), ConfigModule.builder().config(ConfigUtil.readConfig()).build()));
  }

  @Test
  void testGetMessageProcessor() {
    // Act
    MessageProcessor messageProcessor = MessageProcessor.getProcessor(RequestMessageType.SERVICE);

    // Assert
    assertThat(messageProcessor).isInstanceOf(ServiceMessageProcessor.class);
  }

  @Test
  void testGetMessageProcessorForNamespaceProcessor() {
    // Act
    MessageProcessor messageProcessor = MessageProcessor.getProcessor(RequestMessageType.NAMESPACE);

    // Assert
    assertThat(messageProcessor).isInstanceOf(NamespaceMessageProcessor.class);
  }

  @Test
  void testGetProcessorWithNullArgument() {
    assertThrows(
        NullPointerException.class,
        () -> MessageProcessor.getProcessor(this.nullRequestMessageType),
        "Should have thrown NullPointerException");
  }

  @Test
  void testGetMessageProcessorWithWrongType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> MessageProcessor.getProcessor(RequestMessageType.valueOf("WrongType")));
  }
}
