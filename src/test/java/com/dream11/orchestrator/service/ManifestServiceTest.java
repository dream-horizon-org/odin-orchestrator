package com.dream11.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.dto.ManifestServiceDto;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.util.ConfigUtils;
import com.dream11.orchestrator.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class ManifestServiceTest {

  final AppConfig appConfig = ConfigUtils.readConfig();

  final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SneakyThrows
  void testCreatJob() {
    // Arrange
    String componentName = "comp1";
    JSONObject componentAction =
        TestUtil.getComponentAction(componentName, "deploy", TestUtil.getAccountData());
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                List.of(componentAction), TestUtil.getRandomString(), TestUtil.getRandomString()));

    String environmentName = requestBody.getJSONObject("body").getString("environmentName");
    String serviceName = requestBody.getJSONObject("body").getString("serviceName");
    this.objectMapper.readValue(componentAction.toString(), ComponentAction.class);
    ManifestService manifestService = new ManifestService(this.appConfig);
    // Act
    manifestService.init(
        ManifestServiceDto.builder()
            .environmentName(environmentName)
            .serviceName(serviceName)
            .deploymentId(123L)
            .componentAction(
                this.objectMapper.readValue(componentAction.toString(), ComponentAction.class))
            .build());

    Job job = manifestService.createJob();

    // Assert
    Map<String, String> labels =
        Map.of(
            "envName", environmentName,
            "componentName", componentName,
            "componentAction", "deploy",
            "componentActionId", String.valueOf(componentAction.getInt("id")),
            "serviceName", serviceName);

    assertThat(job.getSpec().getTemplate().getMetadata().getLabels())
        .containsExactlyInAnyOrderEntriesOf(labels);
    assertThat(job.getMetadata().getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
    // TODO add more assertions
  }
}
