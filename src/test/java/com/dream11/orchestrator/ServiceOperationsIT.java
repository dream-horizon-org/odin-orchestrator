package com.dream11.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.dto.constants.ResponseMessageType;
import com.dream11.orchestrator.dto.constants.TaskStatus;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.util.ConfigUtils;
import com.dream11.orchestrator.util.TestUtil;
import com.dream11.queue.Message;
import com.dream11.queue.impl.sqs.SqsConfig;
import com.dream11.queue.impl.sqs.SqsConsumer;
import com.dream11.queue.impl.sqs.SqsProducer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@ExtendWith({Setup.class})
class ServiceOperationsIT {

  static SqsAsyncClient SQS_CLIENT;
  static SqsConsumer SQS_RESPONSE_CONSUMER;
  static SqsProducer<String> SQS_REQUEST_PRODUCER;
  static Orchestrator ORCHESTRATOR;

  @BeforeAll
  static void setup() {
    SQS_CLIENT =
        SqsAsyncClient.builder()
            .endpointOverride(URI.create(System.getProperty(Constants.SQS_REQUEST_QUEUE_ENDPOINT)))
            .region(Region.of(System.getProperty(Constants.SQS_REQUEST_QUEUE_REGION)))
            .build();

    SqsConfig sqsRequestConfig =
        SqsConfig.builder()
            .queueUrl(System.getProperty(Constants.SQS_REQUEST_QUEUE_URL))
            .region(System.getProperty(Constants.SQS_REQUEST_QUEUE_REGION))
            .endpoint(System.getProperty(Constants.SQS_REQUEST_QUEUE_ENDPOINT))
            .build();

    SqsConfig sqsResponseConfig =
        SqsConfig.builder()
            .queueUrl(System.getProperty(Constants.SQS_RESPONSE_QUEUE_URL))
            .region(System.getProperty(Constants.SQS_RESPONSE_QUEUE_REGION))
            .endpoint(System.getProperty(Constants.SQS_RESPONSE_QUEUE_ENDPOINT))
            .build();
    SQS_REQUEST_PRODUCER = new SqsProducer<>(sqsRequestConfig, SQS_CLIENT, __ -> __);
    SQS_RESPONSE_CONSUMER = new SqsConsumer(sqsResponseConfig, SQS_CLIENT);
    AppContext.initialize(
        List.of(new MainModule(), ConfigModule.builder().config(ConfigUtils.readConfig()).build()));
    ORCHESTRATOR = AppContext.getInstance(Orchestrator.class);
  }

  @AfterAll
  static void tearDown() {
    SQS_CLIENT.close();
    SQS_REQUEST_PRODUCER.close();
    SQS_RESPONSE_CONSUMER.close();
    ORCHESTRATOR.stop();
  }

  @Test
  @SneakyThrows
  void testServiceOperationFailForInvalidComponentsData() {
    // Arrange
    long taskId = 1;
    this.compressAndSendRequest(
        this.buildRequestMessage(
            taskId, TestUtil.getComponentAction("", "validate", TestUtil.getAccountData())));

    // Act
    ORCHESTRATOR.start();

    // Assert
    this.assertComponentResponse(
        this.getResponse(),
        taskId,
        TaskStatus.FAILED.name(),
        "Invalid component actions list or components list");
    this.assertServiceResponse(
        this.getResponse(),
        taskId,
        TaskStatus.FAILED.name(),
        "Invalid component actions list or components list");
  }

  @Test
  @SneakyThrows
  void testServiceOperationFailForInvalidAccountData() {
    // Arrange
    long taskId = 2;
    this.compressAndSendRequest(
        this.buildRequestMessage(taskId, TestUtil.getComponentAction("comp1", "validate", "{}")));

    // Act
    ORCHESTRATOR.start();

    // Assert
    this.assertComponentResponse(
        this.getResponse(), taskId, TaskStatus.FAILED.name(), "Invalid account object");
    this.assertServiceResponse(
        this.getResponse(), taskId, TaskStatus.FAILED.name(), "Invalid account object");
  }

  @Test
  @SneakyThrows
  void testServiceOperationSuccess() {
    // Arrange
    long taskId = 3;
    this.compressAndSendRequest(
        this.buildRequestMessage(
            taskId, TestUtil.getComponentAction("comp1", "validate", TestUtil.getAccountData())));

    // Act
    ORCHESTRATOR.start();

    // Assert
    this.assertComponentResponse(this.getResponse(), taskId, TaskStatus.SUCCESSFUL.name(), null);
    this.assertServiceResponse(this.getResponse(), taskId, TaskStatus.SUCCESSFUL.name(), null);
  }

  @Test
  @SneakyThrows
  void testServiceOperationFailure() {
    // Arrange
    long taskId = 4;
    this.compressAndSendRequest(
        this.buildRequestMessage(
            taskId,
            TestUtil.getComponentAction("comp2", "deploy", TestUtil.getAccountData(), true)));

    // Act
    ORCHESTRATOR.start();

    // Assert
    this.assertComponentResponse(
        this.getResponse(),
        taskId,
        TaskStatus.FAILED.name(),
        "[1;31mERROR[0;39m [32mError while deploying my_flavour[0;39m ");
    this.assertServiceResponse(this.getResponse(), taskId, TaskStatus.FAILED.name(), null);
  }

  @Test
  @SneakyThrows
  void testServiceOperationFailureDependentComponentAlsoFails() {
    // Arrange
    long taskId = 6;
    Map<String, TaskStatus> componentStatuses =
        Map.of(
            "comp1", TaskStatus.FAILED,
            "comp2", TaskStatus.FAILED,
            "comp3", TaskStatus.SUCCESSFUL);
    Map<String, String> componentErrors =
        Map.of(
            "comp1", "[1;31mERROR[0;39m [32mError while deploying my_flavour[0;39m ",
            "comp2", "Component execution failed due to dependent component failure");

    JSONObject failedComponentAction =
        TestUtil.getComponentAction("comp1", "deploy", TestUtil.getAccountData(), true);
    List<JSONObject> componentActions =
        List.of(
            failedComponentAction,
            TestUtil.getComponentAction("comp2", "deploy", TestUtil.getAccountData())
                .put("dependsOn", List.of(failedComponentAction.getLong("id"))),
            TestUtil.getComponentAction("comp3", "deploy", TestUtil.getAccountData()));
    this.compressAndSendRequest(this.buildRequestMessage(taskId, componentActions));

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject componentResponse = getResponse();
    String componentName = componentResponse.getJSONObject("data").getString("componentName");
    this.assertComponentResponse(
        componentResponse,
        taskId,
        componentStatuses.get(componentName).name(),
        componentErrors.get(componentName));

    componentResponse = getResponse();
    componentName = componentResponse.getJSONObject("data").getString("componentName");
    this.assertComponentResponse(
        componentResponse,
        taskId,
        componentStatuses.get(componentName).name(),
        componentErrors.get(componentName));
    componentResponse = getResponse();
    componentName = componentResponse.getJSONObject("data").getString("componentName");
    this.assertComponentResponse(
        componentResponse,
        taskId,
        componentStatuses.get(componentName).name(),
        componentErrors.get(componentName));

    JSONObject serviceResponse = getResponse();
    this.assertServiceResponse(serviceResponse, taskId, TaskStatus.FAILED.name(), null);
  }

  @SneakyThrows
  private JSONObject getResponse() {
    Message message = SQS_RESPONSE_CONSUMER.receive().get().get(0);
    SQS_RESPONSE_CONSUMER.acknowledgeMessage(message).get();
    return new JSONObject(message.getBody());
  }

  @SneakyThrows
  private void compressAndSendRequest(JSONObject request) {
    SQS_REQUEST_PRODUCER.send(TestUtil.compressAndEncode(request.toString())).get();
  }

  private void assertComponentResponse(
      JSONObject componentResponse, Long taskId, String status, String error) {
    this.assertResponse(
        componentResponse, taskId, status, error, ResponseMessageType.COMPONENT_STATUS.getName());
  }

  private void assertServiceResponse(
      JSONObject componentResponse, Long taskId, String status, String error) {
    this.assertResponse(
        componentResponse, taskId, status, error, ResponseMessageType.SERVICE_STATUS.getName());
  }

  private void assertResponse(
      JSONObject componentResponse, Long taskId, String status, String error, String type) {
    assertThat(componentResponse.getLong("id")).isEqualTo(taskId);
    assertThat(componentResponse.getString("type")).isEqualTo(type);
    assertThat(componentResponse.getString("status")).isEqualTo(status);
    if (Objects.nonNull(error)) {
      assertThat(componentResponse.getString("error")).isEqualTo(error);
    }
  }

  private JSONObject buildRequestMessage(long taskId, JSONObject componentAction) {
    return this.buildRequestMessage(taskId, List.of(componentAction));
  }

  private JSONObject buildRequestMessage(long taskId, List<JSONObject> componentActions) {
    return TestUtil.getServiceRequestMessage(
        TestUtil.getServiceMessageBody(
            componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()),
        taskId);
  }
}
