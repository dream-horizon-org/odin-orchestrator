package com.dream11.orchestrator.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.dto.constants.RequestMessageType;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.util.ConfigUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageProcessorTest {

  @BeforeAll
  static void setup() {
    AppContext.initialize(
        List.of(new MainModule(), ConfigModule.builder().config(ConfigUtils.readConfig()).build()));
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
}
