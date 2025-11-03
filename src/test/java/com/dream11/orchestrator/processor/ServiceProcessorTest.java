package com.dream11.orchestrator.processor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.provisioner.KubernetesRunnerProvisioner;
import com.dream11.orchestrator.service.ExecutorService;
import com.dream11.orchestrator.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceProcessorTest {

  final ObjectMapper objectMapper = AppContext.getObjectMapper();
  @Mock private ExecutorService executorService;
  @Mock private KubernetesRunnerProvisioner kubernetesRunnerProvisioner;
  @InjectMocks private ServiceMessageProcessor serviceMessageProcessor;

  @Mock RequestMessage nullRequestMessage;

  @Test
  @SneakyThrows
  void testProcess() {
    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                List.of(TestUtil.getComponentAction("comp1", "deploy", TestUtil.getAccountData())),
                TestUtil.getRandomString(),
                TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    when(this.kubernetesRunnerProvisioner.namespaceExists(Mockito.anyString())).thenReturn(false);
    doNothing().when(this.executorService).start();

    // Act
    this.serviceMessageProcessor.process(requestMessage);

    // Assert
    verify(this.executorService, times(1)).start();
  }

  @Test
  @SneakyThrows
  void testProcessNamespaceExist() {
    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                List.of(TestUtil.getComponentAction("comp1", "deploy", TestUtil.getAccountData())),
                TestUtil.getRandomString(),
                TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    when(this.kubernetesRunnerProvisioner.namespaceExists(Mockito.anyString())).thenReturn(true);
    doNothing().when(this.executorService).resume();

    // Act
    this.serviceMessageProcessor.process(requestMessage);

    // Assert
    verify(this.executorService, times(1)).resume();
  }

  @Test
  void testProcessWithNullRequestMessage() {

    assertThrows(
        NullPointerException.class,
        () -> this.serviceMessageProcessor.process(this.nullRequestMessage),
        "Should have thrown NullPointerException");
  }
}
