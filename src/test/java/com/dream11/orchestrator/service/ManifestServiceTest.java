package com.dream11.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.dto.ManifestServiceDto;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.dto.request.Stage;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.util.ConfigUtil;
import com.dream11.orchestrator.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class ManifestServiceTest {

  static final AppConfig APP_CONFIG = ConfigUtil.readConfig();

  static final ObjectMapper OBJECT_MAPPER = AppContext.getObjectMapper();
  static String ENVIRONMENT_NAME;
  static String COMPONENT_NAME = "comp1";
  static String SERVICE_NAME;
  static ManifestService MANIFEST_SERVICE;
  static Stage STAGE_SPY;
  static ComponentAction COMPONENT_ACTION;
  static String NAMESPACE;

  @BeforeAll
  @SneakyThrows
  static void setup() {
    String traceId = "trace";
    AppContext.setTraceId(traceId);
    JSONObject componentAction =
        TestUtil.getComponentAction(COMPONENT_NAME, "deploy", TestUtil.getAccountData());
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                List.of(componentAction), TestUtil.getRandomString(), TestUtil.getRandomString()));

    COMPONENT_ACTION = OBJECT_MAPPER.readValue(componentAction.toString(), ComponentAction.class);
    STAGE_SPY = Mockito.spy(COMPONENT_ACTION.getStage());
    COMPONENT_ACTION.setStage(STAGE_SPY);
    ENVIRONMENT_NAME = requestBody.getJSONObject("body").getString("environmentName");
    SERVICE_NAME = requestBody.getJSONObject("body").getString("serviceName");
    MANIFEST_SERVICE = new ManifestService(APP_CONFIG);
    MANIFEST_SERVICE.init(
        ManifestServiceDto.builder()
            .environmentName(ENVIRONMENT_NAME)
            .serviceName(SERVICE_NAME)
            .deploymentId(123L)
            .componentAction(COMPONENT_ACTION)
            .build());

    NAMESPACE = String.format("%s-%s-%d-%s", ENVIRONMENT_NAME, SERVICE_NAME, 123, traceId);
  }

  @AfterAll
  static void tearDown() {
    AppContext.setTraceId(null);
  }

  @Test
  @SneakyThrows
  void testCreatJob() {
    // Act
    Job job = MANIFEST_SERVICE.createJob();

    // Assert
    // Metadata
    assertThat(job.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(job.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(job.getSpec().getTemplate().getMetadata().getLabels())
        .containsExactlyInAnyOrderEntriesOf(this.getExpectedLabels());
    assertThat(job.getMetadata().getLabels())
        .containsExactlyInAnyOrderEntriesOf(this.getExpectedLabels());

    PodSpec podSpec = job.getSpec().getTemplate().getSpec();
    assertThat(podSpec.getShareProcessNamespace()).isTrue();
    assertThat(podSpec.getRestartPolicy()).isEqualTo("Never");
    assertThat(podSpec.getServiceAccountName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(podSpec.getImagePullSecrets()).hasSize(1);
    assertThat(podSpec.getImagePullSecrets()).extracting("name").containsExactly("test");

    // Volumes
    assertThat(podSpec.getVolumes())
        .containsAll(
            APP_CONFIG.getRunner().getHostVolumeMounts().stream()
                .map(
                    hostVolumeMount ->
                        new VolumeBuilder()
                            .withName(hostVolumeMount.getName())
                            .withHostPath(
                                new HostPathVolumeSourceBuilder()
                                    .withType("Directory")
                                    .withPath(hostVolumeMount.getHostPath())
                                    .build())
                            .build())
                .toList());

    assertThat(podSpec.getVolumes())
        .containsAll(
            List.of(
                new VolumeBuilder()
                    .withName(Constants.RUNNER_POD_VOLUME_NAME)
                    .withEmptyDir(new EmptyDirVolumeSource())
                    .build(),
                new VolumeBuilder()
                    .withName(Constants.SHARED_RUNNER_POD_VOLUME_NAME)
                    .withEmptyDir(new EmptyDirVolumeSource())
                    .build()));

    // Container
    assertThat(podSpec.getContainers()).hasSize(2);
    assertThat(podSpec.getContainers())
        .extracting("name")
        .contains(Constants.RUNNER, Constants.DIND);
    this.assertRunnerContainer(podSpec.getContainers().get(0));
    this.assertDinDContainer(podSpec.getContainers().get(1));
  }

  @Test
  void testCreateServiceAccount() {
    // Act
    ServiceAccount serviceAccount = MANIFEST_SERVICE.createServiceAccount();

    // Assert
    assertThat(serviceAccount.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(serviceAccount.getMetadata().getLabels()).isEqualTo(this.getExpectedLabels());
    assertThat(serviceAccount.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(serviceAccount.getMetadata().getAnnotations())
        .containsExactlyInAnyOrderEntriesOf(Map.of("runnerAnnotationKey", "runnerAnnotationValue"));
  }

  @Test
  void testCreateConfigMap() {
    // Act
    ConfigMap configMap = MANIFEST_SERVICE.createConfigMap();

    // Assert
    assertThat(configMap.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(configMap.getMetadata().getLabels()).isEqualTo(this.getExpectedLabels());
    assertThat(configMap.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(configMap.getMetadata().getAnnotations()).isEmpty();
    assertThat(configMap.getData())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "ODIN_COMPONENT_TYPE",
                COMPONENT_ACTION.getType(),
                "ODIN_COMPONENT_VERSION",
                COMPONENT_ACTION.getVersion()));
  }

  @ParameterizedTest
  @CsvSource({"deploy", "operate"})
  void testCreateSecret(String action) {
    // Arrange
    doReturn(action).when(STAGE_SPY).getName();
    // Act
    Secret secret = MANIFEST_SERVICE.createSecret();

    // Assert
    assertThat(secret.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(secret.getMetadata().getLabels()).isEqualTo(this.getExpectedLabels());
    assertThat(secret.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(secret.getMetadata().getAnnotations()).isEmpty();
    assertThat(secret.getStringData()).containsOnlyKeys(this.getSecretExpectedKeys(action));
  }

  private List<String> getSecretExpectedKeys(String action) {
    List<String> expectedKeys =
        new ArrayList<>(
            List.of(
                "ODIN_RUNNER_DIND_ENABLED",
                "ODIN_COMPONENT_REGISTRY_URL",
                "ODIN_COMPONENT_REGISTRY_USERNAME",
                "ODIN_COMPONENT_REGISTRY_PASSWORD",
                "ODIN_DSL_URL",
                "ODIN_DSL_USERNAME",
                "ODIN_DSL_PASSWORD",
                "ODIN_BASE_CONFIG",
                "ODIN_FLAVOUR_CONFIG",
                "ODIN_COMPONENT_METADATA",
                "ODIN_DSL_METADATA",
                "ODIN_CLOUD_PROVIDER",
                "ODIN_CLOUD_PROVIDER_DATA",
                "ODIN_BASE64_ENCODED_KUBECONFIG"));
    if (action.equals("operate")) {
      expectedKeys.add("ODIN_OPERATION_CONFIG");
    }
    return expectedKeys;
  }

  @Test
  void testCreateConfigMapUninitialized() {
    // Arrange
    ManifestService manifestService = new ManifestService(APP_CONFIG);
    // Act & Assert
    assertThatThrownBy(manifestService::createConfigMap)
        .isInstanceOf(OrchestratorException.class)
        .hasMessage("Manifest service object uninitialized. Call init() first");
  }

  private boolean hasVolumeMount(Container c, String volumeName, String mountPath) {
    return c.getVolumeMounts().stream()
        .anyMatch(vm -> volumeName.equals(vm.getName()) && mountPath.equals(vm.getMountPath()));
  }

  private void assertEnvVariables(Container container) {
    assertThat(container.getEnvFrom())
        .extracting(
            e ->
                e.getConfigMapRef() != null
                    ? e.getConfigMapRef().getName()
                    : e.getSecretRef().getName())
        .contains(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(container.getEnv())
        .extracting(EnvVar::getName)
        .containsAll(APP_CONFIG.getRunner().getEnvVars().keySet());
  }

  private void assertResources(Container container) {
    assertThat(container.getResources().getRequests())
        .containsAllEntriesOf(APP_CONFIG.getRunner().getResources().getRequests());
    assertThat(container.getResources().getLimits())
        .containsAllEntriesOf(APP_CONFIG.getRunner().getResources().getLimits());
  }

  private void assertRunnerContainer(Container runner) {
    assertThat(runner.getImage()).isEqualTo(APP_CONFIG.getRunner().getImage());
    assertThat(runner.getImagePullPolicy()).isEqualTo(APP_CONFIG.getRunner().getImagePullPolicy());
    this.assertEnvVariables(runner);
    this.assertResources(runner);
    APP_CONFIG
        .getRunner()
        .getHostVolumeMounts()
        .forEach(
            hostVolumeMount ->
                assertThat(
                        this.hasVolumeMount(
                            runner, hostVolumeMount.getName(), hostVolumeMount.getMountPath()))
                    .isTrue());
    assertThat(this.hasVolumeMount(runner, Constants.RUNNER_POD_VOLUME_NAME, "/run")).isTrue();
    assertThat(this.hasVolumeMount(runner, Constants.SHARED_RUNNER_POD_VOLUME_NAME, "/tmp"))
        .isTrue();
  }

  private void assertDinDContainer(Container dind) {
    assertThat(dind.getImage()).isEqualTo(APP_CONFIG.getRunner().getDind().getImage());
    assertThat(dind.getImagePullPolicy())
        .isEqualTo(APP_CONFIG.getRunner().getDind().getImagePullPolicy());
    assertThat(dind.getArgs())
        .contains("dockerd", "--host=unix:///run/docker.sock", "--group=$(DOCKER_GROUP_GID)");
    assertThat(dind.getSecurityContext().getPrivileged()).isTrue();

    this.assertEnvVariables(dind);
    this.assertResources(dind);
    assertThat(this.hasVolumeMount(dind, Constants.RUNNER_POD_VOLUME_NAME, "/run")).isTrue();
    assertThat(this.hasVolumeMount(dind, Constants.SHARED_RUNNER_POD_VOLUME_NAME, "/tmp")).isTrue();
  }

  private Map<String, String> getExpectedLabels() {
    return Map.of(
        "envName", ENVIRONMENT_NAME,
        "componentName", COMPONENT_NAME,
        "componentAction", STAGE_SPY.getName(),
        "componentActionId", String.valueOf(COMPONENT_ACTION.getId()),
        "serviceName", SERVICE_NAME);
  }
}
