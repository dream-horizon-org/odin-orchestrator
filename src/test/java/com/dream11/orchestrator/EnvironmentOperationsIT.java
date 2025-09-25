package com.dream11.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.constants.NamespaceAction;
import com.dream11.orchestrator.constants.NamespaceProviderType;
import com.dream11.orchestrator.constants.TaskStatus;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.util.ConfigUtils;
import com.dream11.orchestrator.util.TestUtil;
import com.dream11.queue.Message;
import com.dream11.queue.impl.sqs.SqsConfig;
import com.dream11.queue.impl.sqs.SqsConsumer;
import com.dream11.queue.impl.sqs.SqsProducer;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@ExtendWith({Setup.class})
@WireMockTest(httpPort = 8080)
@Slf4j
class EnvironmentOperationsIT {
  static SqsAsyncClient SQS_CLIENT;
  static SqsConsumer SQS_RESPONSE_CONSUMER;
  static SqsProducer<String> SQS_REQUEST_PRODUCER;
  static Orchestrator ORCHESTRATOR;

  static final String KUBECONFIG_PATH = "kubeconfig.yaml";
  static final String BASE64_ENCODED_KUBECONFIG = "BASE64_ENCODED_KUBECONFIG";

  static KubernetesClient KUBERNETES_CLIENT;

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
    createKubeconfig();
    KUBERNETES_CLIENT = TestUtil.createKubernetesClient(KUBECONFIG_PATH);
  }

  @BeforeEach
  void setupBeforeEach() {
    AppContext.reset();
    try {
      AppContext.initialize(
          List.of(
              new MainModule(), ConfigModule.builder().config(ConfigUtils.readConfig()).build()));
    } catch (OrchestratorException e) {
      // Ignored intentionally
    }
    ORCHESTRATOR = AppContext.getInstance(Orchestrator.class);
  }

  @SneakyThrows
  private JSONObject getResponse() {
    Message message = SQS_RESPONSE_CONSUMER.receive().get().get(0);
    SQS_RESPONSE_CONSUMER.acknowledgeMessage(message).get();
    return new JSONObject(message.getBody());
  }

  @Test
  @SneakyThrows
  void testCreateEnvFailureForInvalidNamespaceProvider() {
    // Arrange

    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                Map.of("NAMESPACE_PROVIDER", "dummy"),
                NamespaceAction.CREATE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();
    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.FAILED.name());
    assertThat(response.getString("error"))
        .contains(
            "Cannot deserialize value of type `com.dream11.orchestrator.constants.NamespaceProviderType` from String \"dummy\": not one of the values accepted for Enum class: [ODIN]");
  }

  @Test
  @SneakyThrows
  void testCreateEnvFailureForInvalidAction() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                Map.of("NAMESPACE_PROVIDER", NamespaceProviderType.ODIN.name()),
                "dummy"))
        .get();

    // Act & Assert
    assertThatThrownBy(() -> ORCHESTRATOR.start())
        .isInstanceOf(Exception.class)
        .hasMessageContaining(
            "not one of the values accepted for Enum class: [CREATE_ENVIRONMENT, DELETE_ENVIRONMENT]");
  }

  @Test
  @SneakyThrows
  void testCreateEnvSuccessfulForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                this.getTemplateData(),
                NamespaceAction.CREATE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.SUCCESSFUL.name());
    Optional<Namespace> actualNamespace = TestUtil.getNamespace(KUBERNETES_CLIENT, namespace);
    assertThat(actualNamespace)
        .isNotEmpty()
        .get()
        .satisfies(
            ns ->
                assertThat(ns.getMetadata().getAnnotations())
                    .containsEntry("annotationKey", "annotationValue"))
        .satisfies(
            ns ->
                assertThat(ns.getMetadata().getLabels())
                    .containsEntry("provisioned-by-user", "odin"));
    Optional<Deployment> secret =
        TestUtil.getDeployment(KUBERNETES_CLIENT, "hello-world", namespace);
    assertThat(secret).isNotEmpty();
    TestUtil.deleteNamespace(KUBERNETES_CLIENT, namespace);
  }

  @Test
  @SneakyThrows
  void testCreateEnvSuccessfulExistingForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    // Create namespace
    TestUtil.createNamespace(KUBERNETES_CLIENT, namespace);
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                this.getTemplateData(),
                NamespaceAction.CREATE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.SUCCESSFUL.name());
    Optional<Namespace> actualNamespace = TestUtil.getNamespace(KUBERNETES_CLIENT, namespace);
    assertThat(actualNamespace)
        .isNotEmpty()
        .get()
        .satisfies(
            ns ->
                assertThat(ns.getMetadata().getAnnotations())
                    .containsEntry("annotationKey", "annotationValue"))
        .satisfies(
            ns ->
                assertThat(ns.getMetadata().getLabels())
                    .containsEntry("provisioned-by-user", "odin"));
    Optional<Deployment> secret =
        TestUtil.getDeployment(KUBERNETES_CLIENT, "hello-world", namespace);
    assertThat(secret).isNotEmpty();
    TestUtil.deleteNamespace(KUBERNETES_CLIENT, namespace);
  }

  @Test
  @SneakyThrows
  void testCreateEnvFailureForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString().toUpperCase();
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                this.getTemplateData(),
                NamespaceAction.CREATE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.FAILED.name());
    assertThat(response.getString("error"))
        .contains(
            "a lowercase RFC 1123 label must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character");
  }

  @Test
  @SneakyThrows
  void testCreateEnvHelmFailureForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    Map<String, Object> templateData = new HashMap<>(this.getTemplateData());
    templateData.put("CHART_VERSION", "non-existent-chart");

    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId, namespace, templateData, NamespaceAction.CREATE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.FAILED.name());
    assertThat(response.getString("error"))
        .contains("Error: chart \"hello-world\" version \"non-existent-chart\" not found");
    TestUtil.deleteNamespace(KUBERNETES_CLIENT, namespace);
  }

  @Test
  @SneakyThrows
  void testDeleteEnvSuccessfulForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();

    // Create namespace
    TestUtil.createNamespace(KUBERNETES_CLIENT, namespace);
    // Send delete message
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                this.getTemplateData(),
                NamespaceAction.DELETE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.SUCCESSFUL.name());
    Optional<Namespace> actualNamespace = TestUtil.getNamespace(KUBERNETES_CLIENT, namespace);
    actualNamespace.ifPresent(
        value ->
            assertThat(value)
                .satisfies(
                    ns ->
                        assertThat(ns.getMetadata().getDeletionTimestamp())
                            .isNotNull()
                            .isNotEmpty()));
  }

  @Test
  @SneakyThrows
  void testDeleteEnvNonExistingSuccessfulForOdinProvider() {
    // Arrange
    long taskId = TestUtil.generateIntegerUUID();
    String namespace = TestUtil.getRandomString();
    SQS_REQUEST_PRODUCER
        .send(
            this.buildAndCompressRequestMessage(
                taskId,
                namespace,
                this.getTemplateData(),
                NamespaceAction.DELETE_ENVIRONMENT.name()))
        .get();

    // Act
    ORCHESTRATOR.start();

    // Assert
    JSONObject response = this.getResponse();
    assertThat(response.getString("type")).isEqualTo("NAMESPACE");
    assertThat(response.getLong("id")).isEqualTo(taskId);
    assertThat(response.getString("status")).isEqualTo(TaskStatus.SUCCESSFUL.name());
  }

  private Map<String, Object> getTemplateData() {
    return Map.of(
        "NAMESPACE_PROVIDER",
        NamespaceProviderType.ODIN.name(),
        BASE64_ENCODED_KUBECONFIG,
        System.getenv(BASE64_ENCODED_KUBECONFIG));
  }

  private String buildAndCompressRequestMessage(
      long taskId, String namespace, Map<String, Object> accountTemplateData, String action) {
    return TestUtil.compressAndEncode(
        TestUtil.getNamespaceRequestMessage(
                TestUtil.getNamespaceMessageBody(
                    namespace, action, TestUtil.getAccountData(accountTemplateData)),
                taskId)
            .toString());
  }

  @SneakyThrows
  static void createKubeconfig() {
    FileUtils.writeStringToFile(
        new File(KUBECONFIG_PATH),
        new String(Base64.getDecoder().decode(System.getenv(BASE64_ENCODED_KUBECONFIG))),
        Charset.defaultCharset());
  }
}
