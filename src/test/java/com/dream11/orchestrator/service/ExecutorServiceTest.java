package com.dream11.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.provisioner.KubernetesRunnerProvisioner;
import com.dream11.queue.producer.MessageProducer;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExecutorServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  KubernetesRunnerProvisioner kubernetesRunnerProvisioner;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ManifestService manifestService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DiscoveryClientService discoveryClientService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MessageProducer<String> messageProducer;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ServiceRequestMessageBody serviceRequestMessageBody;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ComponentAction componentAction;

  private ExecutorService executorService;

  @BeforeEach
  void setup() {
    this.executorService =
        new ExecutorService(
            kubernetesRunnerProvisioner, manifestService, discoveryClientService, messageProducer);
  }

  @Test
  void testCleanup() {

    assertThrows(
        OrchestratorException.class,
        () -> this.executorService.checkInit(),
        "Should have thrown OrchestratorException");

    this.executorService.init(0, "namespace", this.serviceRequestMessageBody);
    this.executorService.checkInit();
    this.executorService.cleanup();
    this.executorService.resume();

    this.executorService.checkStateAndUpdateDag(Set.of(this.componentAction));

    this.executorService.updateJobStatus(
        this.componentAction.getName(),
        this.componentAction.getId(),
        Constants.JOB_FAILED,
        "validate",
        "");

    this.componentAction.setOperationConfig(Map.of());
    this.executorService.doExecute(Set.of(this.componentAction));
  }
}
