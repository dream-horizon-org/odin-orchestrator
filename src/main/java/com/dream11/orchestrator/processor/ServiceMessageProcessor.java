package com.dream11.orchestrator.processor;

import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.provisioner.KubernetesRunnerProvisioner;
import com.dream11.orchestrator.service.ExecutorService;
import com.dream11.orchestrator.util.AccountUtils;
import com.dream11.orchestrator.util.ManifestUtils;
import com.dream11.orchestrator.util.ServiceUtils;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ServiceMessageProcessor implements MessageProcessor {

  final ExecutorService executorService;

  final KubernetesRunnerProvisioner kubernetesRunnerProvisioner;

  @Override
  @SneakyThrows
  public void process(RequestMessage requestMessage) {
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();
    String deploymentNamespace =
        ManifestUtils.getNamespace(
            serviceRequestMessageBody.getEnvName(),
            serviceRequestMessageBody.getServiceName(),
            requestMessage.getId());
    ServiceUtils.validateComponents(serviceRequestMessageBody.getComponentActions());
    AccountUtils.validateAccount(serviceRequestMessageBody.getComponentActions());
    this.executorService.init(
        requestMessage.getId(), deploymentNamespace, serviceRequestMessageBody);
    // Check namespace
    if (this.kubernetesRunnerProvisioner.namespaceExists(deploymentNamespace)) {
      this.executorService.resume();
    } else {
      this.executorService.start();
    }
  }
}
