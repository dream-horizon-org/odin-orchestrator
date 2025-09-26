package com.dream11.orchestrator;

import static com.dream11.orchestrator.constants.Constants.DEPLOY_ACTION_NAME;
import static com.dream11.orchestrator.constants.Constants.UNDEPLOY_ACTION_NAME;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.DISCOVERY_SERVICE_ERROR;
import static com.dream11.orchestrator.service.DiscoveryClientService.DISCOVERY_ENDPOINT;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.account.AccountDto;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.service.DiscoveryClientService;
import com.dream11.orchestrator.util.ConfigUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 8081)
class DiscoveryClientServiceIT {

  @Inject AppConfig appConfig;

  DiscoveryClientService discoveryClientService;

  @BeforeEach
  void setup() {
    stubFor(
        put(urlEqualTo(DISCOVERY_ENDPOINT))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"responseList\":[{\"status\":\"SUCCESSFUL\",\"message\":null,\"id\":\"1\"}]}")));
    Guice.createInjector(
            new MainModule(),
            ConfigModule.builder().config(ConfigUtil.readConfig()).build(),
            BoundFieldModule.of(this))
        .injectMembers(this);

    this.discoveryClientService = new DiscoveryClientService(this.appConfig, this.getHttpClient());
  }

  @Test
  void testDiscoveryClientServiceDeploy() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(Map.of("discovery", Map.of("private", "test-route.private")));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        DEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert("test-route.private", "test-value", "1"))));
  }

  @Test
  void testDiscoveryClientServiceDeployMultipleKeys() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(
        Map.of(
            "discovery", Map.of("private", "test-route.private", "public", "test-route.public")));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value" , "public":"test-public-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        DEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert("test-route.private", "test-value", "1"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert(
                        "test-route.public", "test-public-value", "1"))));
  }

  @Test
  void testDiscoveryClientServiceDeployWithMultipleRoutes() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(
        Map.of(
            "discovery",
            Map.of(
                "private",
                List.of("test-route1.private", "test-route2.private", "test-route3.private"))));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        DEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert(
                        "test-route1.private", "test-value", "1"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert(
                        "test-route2.private", "test-value", "2"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(
                    createDiscoveryRequestBodyForUpsert(
                        "test-route3.private", "test-value", "3"))));
  }

  @Test
  void testDiscoveryClientServiceJobUndeploy() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(Map.of("discovery", Map.of("private", "test-route.private")));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        UNDEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route.private", "1"))));
  }

  @Test
  void testDiscoveryClientServiceUndeployMultipleKeys() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(
        Map.of(
            "discovery", Map.of("private", "test-route.private", "public", "test-route.public")));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value" , "public":"test-public-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        UNDEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route.private", "1"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route.public", "1"))));
  }

  @Test
  void testDiscoveryClientServiceError() {
    // Arrange
    stubFor(
        put(urlEqualTo(DISCOVERY_ENDPOINT))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withBody(
                        "{\"responseList\":[{\"status\":\"FAILED\",\"message\":\"Record not present\",\"id\":\"1\"}]}")));

    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(Map.of("discovery", Map.of("private", "test-route.private")));
    OrchestratorException exception =
        assertThrows(
            OrchestratorException.class,
            () -> {
              discoveryClientService.handleDiscovery(
                  1,
                  componentAction,
                  """
                      ------ODIN-DISCOVERY-MARKER-START------
                      {"private":"test-value"}
                      ------ODIN-DISCOVERY-MARKER-END------
                      """,
                  DEPLOY_ACTION_NAME);
            });

    assertThat(
            DISCOVERY_SERVICE_ERROR.getErrorMessage().replace("%s", "")
                + "service returned status code: 400 response: "
                + "{\"responseList\":[{\"status\":\"FAILED\",\"message\":\"Record not present\",\"id\":\"1\"}]}")
        .isEqualTo(exception.getMessage());
  }

  @Test
  void testDiscoveryClientResponseError() {
    // Arrange
    stubFor(
        put(urlEqualTo(DISCOVERY_ENDPOINT))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        "{\"responseList\":{\"status\":\"FAILED\",\"message\":\"Record not present\",\"id\":\"1\"}]}")));

    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(Map.of("discovery", Map.of("private", "test-route.private")));
    OrchestratorException exception =
        assertThrows(
            OrchestratorException.class,
            () -> {
              discoveryClientService.handleDiscovery(
                  1,
                  componentAction,
                  """
                      ------ODIN-DISCOVERY-MARKER-START------
                      {"private":"test-value"}
                      ------ODIN-DISCOVERY-MARKER-END------
                      """,
                  DEPLOY_ACTION_NAME);
            });

    assertThat(exception.getMessage()).contains("Unexpected close marker ']': expected '}'");
  }

  @Test
  void testDiscoveryClientServiceUndeployWithMultipleRoutes() throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction = createComponentAction();
    componentAction.setBaseConfig(
        Map.of(
            "discovery",
            Map.of(
                "private",
                List.of("test-route1.private", "test-route2.private", "test-route3.private"))));

    // Act
    this.discoveryClientService.handleDiscovery(
        1,
        componentAction,
        """
            ------ODIN-DISCOVERY-MARKER-START------
            {"private":"test-value"}
            ------ODIN-DISCOVERY-MARKER-END------
                """,
        UNDEPLOY_ACTION_NAME);

    // Assert
    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route1.private", "1"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route2.private", "2"))));

    verify(
        1,
        putRequestedFor(urlEqualTo(DISCOVERY_ENDPOINT))
            .withRequestBody(
                equalTo(createDiscoveryRequestBodyForDelete("test-route3.private", "3"))));
  }

  public HttpClient getHttpClient() {
    return HttpClients.createMinimal();
  }

  ComponentAction createComponentAction() {
    ComponentAction componentAction = new ComponentAction();
    AccountDto accountDto = new AccountDto();
    Account account = new Account();
    account.setName("staging");
    accountDto.setAccount(account);
    componentAction.setAccounts(accountDto);
    return componentAction;
  }

  String createDiscoveryRequestBodyForUpsert(String key, String value, String id) {
    return "{\"accountName\":\"staging\",\"recordActions\":[{\"action\":\"UPSERT\",\"id\":\""
        + id
        + "\",\"record\":{\"name\":\""
        + key
        + "\","
        + "\"values\":[\""
        + value
        + "\"]}}]}";
  }

  String createDiscoveryRequestBodyForDelete(String key, String id) {
    return "{\"accountName\":\"staging\",\"recordActions\":[{\"action\":\"DELETE\",\"id\":\""
        + id
        + "\",\"record\":{\"name\":\""
        + key
        + "\""
        + "}}]}";
  }
}
