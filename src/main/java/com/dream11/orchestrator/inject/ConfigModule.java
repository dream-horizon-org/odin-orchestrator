package com.dream11.orchestrator.inject;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.queue.consumer.MessageConsumer;
import com.dream11.queue.consumer.MessageConsumerFactory;
import com.dream11.queue.producer.MessageProducer;
import com.dream11.queue.producer.MessageProducerFactory;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ConfigModule extends AbstractModule {

  @NonNull final AppConfig config;
  MessageConsumer messageConsumer;
  MessageProducer<String> messageProducer;

  private void init() {
    this.messageProducer = MessageProducerFactory.create(config.getQueue().getResponse());
    this.messageConsumer = MessageConsumerFactory.create(config.getQueue().getRequest());
  }

  @Override
  protected void configure() {
    this.init();
    bind(AppConfig.class).toInstance(config);
    bind(MessageConsumer.class).toInstance(this.messageConsumer);
    bind(new TypeLiteral<MessageProducer<String>>() {}).toInstance(this.messageProducer);
  }
}
