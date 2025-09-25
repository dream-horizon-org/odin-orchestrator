package com.dream11.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.dto.ManifestServiceDto;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.util.ConfigUtils;
import com.dream11.orchestrator.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ManifestServiceTest {

  static final AppConfig APP_CONFIG = ConfigUtils.readConfig();

  static final ObjectMapper OBJECT_MAPPER = AppContext.getObjectMapper();
  static String ENVIRONMENT_NAME;
  static String COMPONENT_NAME = "comp1";
  static String SERVICE_NAME;
  static ManifestService MANIFEST_SERVICE;
  static ComponentAction COMPONENT_ACTION;
  static Map<String, String> LABELS;
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

    LABELS =
        Map.of(
            "envName", ENVIRONMENT_NAME,
            "componentName", COMPONENT_NAME,
            "componentAction", "deploy",
            "componentActionId", String.valueOf(COMPONENT_ACTION.getId()),
            "serviceName", SERVICE_NAME);
    NAMESPACE = String.format("%s-%s-%d-%s", ENVIRONMENT_NAME, SERVICE_NAME, 123, traceId);
  }

  @AfterAll
  static void cleanup() {
    AppContext.setTraceId(null);
  }

  @Test
  @SneakyThrows
  void testCreatJob() {
    // Act
    Job job = MANIFEST_SERVICE.createJob();

    // Assert
    assertThat(job.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(job.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(job.getSpec().getTemplate().getMetadata().getLabels())
        .containsExactlyInAnyOrderEntriesOf(LABELS);
    assertThat(job.getMetadata().getLabels()).containsExactlyInAnyOrderEntriesOf(LABELS);
    // TODO add more assertions
  }

  @Test
  void testCreateServiceAccount() {
    // Act
    ServiceAccount serviceAccount = MANIFEST_SERVICE.createServiceAccount();

    // Assert
    assertThat(serviceAccount.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(serviceAccount.getMetadata().getLabels()).isEqualTo(LABELS);
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
    assertThat(configMap.getMetadata().getLabels()).isEqualTo(LABELS);
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

  @Test
  void testCreateSecret() {
    // Act
    Secret secret = MANIFEST_SERVICE.createSecret();

    // Assert
    assertThat(secret.getMetadata().getName())
        .isEqualTo(COMPONENT_NAME + "-" + COMPONENT_ACTION.getId());
    assertThat(secret.getMetadata().getLabels()).isEqualTo(LABELS);
    assertThat(secret.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(secret.getMetadata().getAnnotations()).isEmpty();
  }
}
