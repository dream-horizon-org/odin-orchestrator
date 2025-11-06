package com.dream11.orchestrator.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.constants.TaskStatus;
import com.dream11.orchestrator.dto.request.RequestMessage;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DagBuilderTest {

  private DagBuilder dagBuilder;
  private final ObjectMapper objectMapper = AppContext.getObjectMapper();

  @BeforeEach
  void setUp() {
    this.dagBuilder = new DagBuilder();
  }

  @ParameterizedTest
  @MethodSource("componentActions")
  @SneakyThrows
  void testClearConnectedComponentsRemovesConnectedComponentsCorrectly(
      List<JSONObject> componentActions, Integer componentId, Set<Integer> expectedIds) {
    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();

    // Act
    this.dagBuilder.init(serviceRequestMessageBody);
    this.dagBuilder.clearConnectedComponents(componentId);

    // Assert
    assertThat(this.dagBuilder.isEmpty()).isFalse();
    assertThat(this.dagBuilder.getNextComponentActionIds()).isEqualTo(new HashSet<>(expectedIds));
  }

  private static Stream<Arguments> componentActions() {
    JSONObject componentAction1 = TestUtil.getComponentAction("comp1", "deploy", "{}");
    JSONObject componentAction2 = TestUtil.getComponentAction("comp2", "deploy", "{}");
    componentAction2.put("dependsOn", List.of(componentAction1.getLong("id")));
    JSONObject componentAction3 = TestUtil.getComponentAction("comp3", "deploy", "{}");
    JSONObject componentAction4 = TestUtil.getComponentAction("comp4", "deploy", "{}");
    JSONObject componentAction5 = TestUtil.getComponentAction("comp4", "deploy", "{}");
    componentAction5.put("dependsOn", List.of(componentAction3.getLong("id")));
    return Stream.of(
        Arguments.of(
            List.of(componentAction1, componentAction2, componentAction3, componentAction4),
            componentAction1.get("id"),
            new HashSet<>(Set.of(componentAction3.getInt("id"), componentAction4.getInt("id")))),
        Arguments.of(
            List.of(componentAction1, componentAction2, componentAction3, componentAction5),
            componentAction1.get("id"),
            new HashSet<>(Set.of(componentAction3.getInt("id")))));
  }

  @Test
  void testDagBuilderReset() {
    // Act
    this.dagBuilder.reset();

    // Assert
    assertThat(this.dagBuilder.isEmpty()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("componentActions")
  @SneakyThrows
  void testIllegalInit(List<JSONObject> componentActions, Integer componentId) {

    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();

    // Assert
    assertThatThrownBy(() -> dagBuilder.updateCompletedNode(1, TaskStatus.SUCCESSFUL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Graph not initialized. Use init() first");
  }

  @ParameterizedTest
  @MethodSource("componentActions")
  @SneakyThrows
  void testDoubleInit(List<JSONObject> componentActions, Integer componentId) {
    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();

    // Act
    this.dagBuilder.init(serviceRequestMessageBody);

    // Assert
    assertThatThrownBy(() -> this.dagBuilder.init(null))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Graph already initialized");
  }

  @ParameterizedTest
  @MethodSource("componentActions")
  @SneakyThrows
  void testUpdateCompletedNodeWithVisibleNode(
      List<JSONObject> componentActions, Integer componentId) {

    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();
    this.dagBuilder.init(serviceRequestMessageBody);

    // Assert
    assertThatThrownBy(
            () -> this.dagBuilder.updateCompletedNode(componentId, TaskStatus.SUCCESSFUL))
        .isInstanceOf(OrchestratorException.class)
        .hasMessageContaining("Cannot update visible graph node comp1");
  }

  @ParameterizedTest
  @MethodSource("componentActions")
  @SneakyThrows
  void testUpdateCompletedNodeWithInvisibleNode(
      List<JSONObject> componentActions, Integer componentId) {

    // Arrange
    JSONObject requestBody =
        TestUtil.getServiceRequestMessage(
            TestUtil.getServiceMessageBody(
                componentActions, TestUtil.getRandomString(), TestUtil.getRandomString()));
    RequestMessage requestMessage =
        this.objectMapper.readValue(requestBody.toString(), RequestMessage.class);
    ServiceRequestMessageBody serviceRequestMessageBody =
        (ServiceRequestMessageBody) requestMessage.getBody();
    this.dagBuilder.init(serviceRequestMessageBody);

    // Act
    this.dagBuilder.setVisibilityFalse(componentId);
    this.dagBuilder.updateCompletedNode(componentId, TaskStatus.FAILED);

    // Assert
    assertThat(this.dagBuilder.executionSuccess()).isFalse();
  }
}
