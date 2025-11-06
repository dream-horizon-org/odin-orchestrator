package com.dream11.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.provisioner.KubernetesRunnerProvisioner;
import com.dream11.queue.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutorServiceTest {

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

  final String namespace = "namespace";

  private ExecutorService executorService;

  @BeforeEach
  void setup() {
    this.executorService =
        new ExecutorService(
            kubernetesRunnerProvisioner, manifestService, discoveryClientService, messageProducer);
  }

  @Test
  void testCheckInitFalseCase() {

    // Act && Assert
    assertThatThrownBy(() -> this.executorService.checkInit())
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Deployer service object uninitialized. Call init() first");
  }

  @Test
  void testCleanUp() {
    // Arrange
    this.executorService.init(0, namespace, this.serviceRequestMessageBody);
    this.executorService.checkInit();

    // Act
    this.executorService.cleanup();

    // Assert
    assertThat(this.kubernetesRunnerProvisioner.namespaceExists(namespace)).isFalse();
  }
}
