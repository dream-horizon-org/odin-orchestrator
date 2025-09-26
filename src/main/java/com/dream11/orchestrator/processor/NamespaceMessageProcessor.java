package com.dream11.orchestrator.processor;

import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.constants.ResponseMessageType;
import com.dream11.orchestrator.constants.TaskStatus;
import com.dream11.orchestrator.dto.ResponseMessage;
import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import com.dream11.orchestrator.dto.request.NamespaceRequestMessageBody;
import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.provisioner.NamespaceProvider;
import com.dream11.orchestrator.provisioner.NamespaceProviderFactory;
import com.dream11.orchestrator.util.AccountUtil;
import com.dream11.orchestrator.util.ApplicationUtil;
import com.dream11.queue.producer.MessageProducer;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class NamespaceMessageProcessor implements MessageProcessor {

  final MessageProducer<String> messageProducer;

  @Override
  @SneakyThrows
  public void process(RequestMessage requestMessage) {
    ResponseMessage.ResponseMessageBuilder responseMessageBuilder =
        ResponseMessage.builder()
            .id(requestMessage.getId())
            .type(ResponseMessageType.NAMESPACE)
            .status(TaskStatus.SUCCESSFUL)
            .error(StringUtils.EMPTY);
    NamespaceRequestMessageBody namespaceRequestMessageBody =
        (NamespaceRequestMessageBody) requestMessage.getBody();

    try {
      K8sServiceData k8sServiceData =
          AccountUtil.getServiceWithCategory(
              namespaceRequestMessageBody.getAccount().getServices(),
              Constants.CATEGORY_KUBERNETES,
              K8sServiceData.class);
      ApplicationUtil.validate(k8sServiceData);
      if (k8sServiceData.getClusters().isEmpty()) {
        log.info(
            String.format(
                "No clusters found for account %s with id %d. Skipping namespace operation",
                namespaceRequestMessageBody.getAccount().getName(), requestMessage.getId()));
        return;
      }
      NamespaceProvider namespaceProvider =
          NamespaceProviderFactory.getProvider(
              k8sServiceData
                  .getClusters()
                  .get(0)
                  .getNamespaceConfig()
                  .getProvider()
                  .getName()); // Assumption namespace providers for all clusters are same
      switch (namespaceRequestMessageBody.getAction()) {
        case CREATE_ENVIRONMENT -> namespaceProvider.createNamespace(
            namespaceRequestMessageBody.getName(),
            k8sServiceData.getClusters(),
            AccountUtil.getResourceLabels(namespaceRequestMessageBody.getAccount()),
            namespaceRequestMessageBody.getOrgId());

        case DELETE_ENVIRONMENT -> namespaceProvider.deleteNamespace(
            namespaceRequestMessageBody.getName(),
            k8sServiceData.getClusters(),
            namespaceRequestMessageBody.getOrgId());
        default -> throw new IllegalArgumentException(
            String.format("Action %s not supported", namespaceRequestMessageBody.getAction()));
      }

    } catch (Exception e) {
      log.error(
          String.format(
              "Namespace operation failed for %s with id %d",
              namespaceRequestMessageBody.getName(), requestMessage.getId()),
          e);
      responseMessageBuilder.error(e.getMessage()).status(TaskStatus.FAILED);
    }

    ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessageBuilder.build());
  }
}
