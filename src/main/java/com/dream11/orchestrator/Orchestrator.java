package com.dream11.orchestrator;

import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.constants.RequestMessageType;
import com.dream11.orchestrator.constants.ResponseMessageType;
import com.dream11.orchestrator.constants.TaskStatus;
import com.dream11.orchestrator.dto.NamespaceResponseData;
import com.dream11.orchestrator.dto.ResponseMessage;
import com.dream11.orchestrator.dto.ServiceResponseData;
import com.dream11.orchestrator.dto.request.NamespaceRequestMessageBody;
import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.processor.MessageProcessor;
import com.dream11.orchestrator.util.ApplicationUtil;
import com.dream11.queue.Message;
import com.dream11.queue.consumer.MessageConsumer;
import com.dream11.queue.producer.MessageProducer;
import com.google.inject.Inject;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Orchestrator {

  private final MessageConsumer messageConsumer;
  private final MessageProducer<String> messageProducer;

  @SneakyThrows
  public void start() {
    Message receivedMessage = this.fetchMessage();
    try {
      this.handleMessage(receivedMessage, receivedMessage::getBody);
    } catch (Exception e) {
      log.error("Error while handling message", e);
      String decompressedMessage = ApplicationUtil.decodeAndDecompress(receivedMessage.getBody());
      RequestMessage requestMessage =
          AppContext.getObjectMapper().readValue(decompressedMessage, RequestMessage.class);
      this.setTraceId(requestMessage);
      ResponseMessage baseResponse =
          ResponseMessage.builder()
              .id(requestMessage.getId())
              .executionId(requestMessage.getTraceId())
              .status(TaskStatus.FAILED)
              .error(e.getMessage())
              .build();
      // Send namespace failed response message
      if (requestMessage.getType().equals(RequestMessageType.NAMESPACE)) {
        ResponseMessage responseMessage =
            baseResponse.toBuilder()
                .type(ResponseMessageType.NAMESPACE)
                .data(
                    NamespaceResponseData.builder()
                        .accountName(
                            ((NamespaceRequestMessageBody) requestMessage.getBody())
                                .getAccount()
                                .getName())
                        .build())
                .build();
        ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessage);
      } else {
        ServiceRequestMessageBody serviceRequestMessageBody =
            (ServiceRequestMessageBody) requestMessage.getBody();
        // send failed response message for all component actions
        serviceRequestMessageBody
            .getComponentActions()
            .forEach(
                componentAction -> {
                  ResponseMessage componentResponseMessage =
                      baseResponse.toBuilder()
                          .type(ResponseMessageType.COMPONENT_STATUS)
                          .data(
                              ServiceResponseData.builder()
                                  .componentName(componentAction.getName())
                                  .stage(componentAction.getStage().getName())
                                  .build())
                          .build();
                  ApplicationUtil.sendResponseMessage(
                      this.messageProducer, componentResponseMessage);
                });
        // Send service failed response message
        ResponseMessage.ResponseMessageBuilder serviceResponseMessage =
            baseResponse.toBuilder().type(ResponseMessageType.SERVICE_STATUS);
        // Check if any component action is in validate stage
        boolean isValidateStage =
            serviceRequestMessageBody.getComponentActions().stream()
                .anyMatch(
                    componentAction ->
                        componentAction.getStage().getName().equalsIgnoreCase(Constants.VALIDATE));
        // If stage is validate then set stage in response data
        if (isValidateStage) {
          serviceResponseMessage.data(
              ServiceResponseData.builder().stage(Constants.VALIDATE).build());
        }
        ApplicationUtil.sendResponseMessage(this.messageProducer, serviceResponseMessage.build());
      }
    } finally {
      this.messageConsumer.acknowledgeMessage(receivedMessage).get();
      log.info("Acknowledged message:{}", receivedMessage);
    }
  }

  @SneakyThrows
  public void stop() {
    this.messageConsumer.close();
    this.messageProducer.close();
  }

  @SneakyThrows
  private void handleMessage(Message receivedMessage, Supplier<String> messageSupplier) {
    if (receivedMessage != null) {
      // Decode and decompress message
      String decompressedMessage = ApplicationUtil.decodeAndDecompress(messageSupplier.get());
      // Process message according to type
      RequestMessage requestMessage =
          AppContext.getObjectMapper().readValue(decompressedMessage, RequestMessage.class);
      this.setTraceId(requestMessage);
      ApplicationUtil.validate(requestMessage);
      MessageProcessor.getProcessor(requestMessage.getType()).process(requestMessage);
    }
  }

  private void setTraceId(RequestMessage requestMessage) {
    // Add traceId to MDC if present in request message
    if (requestMessage.getTraceId() != null) {
      AppContext.setTraceId(requestMessage.getTraceId());
    }
  }

  @SneakyThrows
  private Message fetchMessage() {
    List<Message> messages;
    do {
      log.debug("Waiting for message");
      messages = this.messageConsumer.receive(Constants.MESSAGE_TIMEOUT_SECONDS).get();
    } while (messages.isEmpty());
    log.info("Received message: {}", messages.get(0).getBody());
    return messages.get(0);
  }
}
