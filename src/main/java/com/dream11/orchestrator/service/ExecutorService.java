package com.dream11.orchestrator.service;

import static com.dream11.orchestrator.constants.Constants.ODIN_DISCOVERY_MARKER_END;
import static com.dream11.orchestrator.constants.Constants.ODIN_DISCOVERY_MARKER_START;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.EXECUTOR_SERVICE_UNINITIALIZED;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.RUNNER_DISCOVERY_OUTPUT_NOT_FOUND;

import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.constants.ResponseMessageType;
import com.dream11.orchestrator.constants.TaskStatus;
import com.dream11.orchestrator.dto.ManifestServiceDto;
import com.dream11.orchestrator.dto.ResponseMessage;
import com.dream11.orchestrator.dto.ServiceResponseData;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.dto.request.ServiceRequestMessageBody;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.graph.DagBuilder;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.provisioner.KubernetesRunnerProvisioner;
import com.dream11.orchestrator.util.ApplicationUtil;
import com.dream11.orchestrator.util.ManifestUtil;
import com.dream11.orchestrator.util.ServiceUtil;
import com.dream11.queue.producer.MessageProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ExecutorService {
  private DagBuilder dagBuilder;
  private long deploymentId;
  final KubernetesRunnerProvisioner kubernetesRunnerProvisioner;
  final ManifestService manifestService;
  final DiscoveryClientService discoveryClientService;
  final MessageProducer<String> messageProducer;

  @Getter private String namespace;
  @Getter private ServiceRequestMessageBody serviceRequestMessageBody;

  public void init(
      long deploymentId, String namespace, ServiceRequestMessageBody serviceRequestMessageBody) {
    this.namespace = namespace;
    this.deploymentId = deploymentId;
    this.serviceRequestMessageBody = serviceRequestMessageBody;

    // Initialize component dependency graph
    this.dagBuilder = new DagBuilder();
    this.dagBuilder.init(serviceRequestMessageBody);
  }

  void checkInit() {
    if (this.serviceRequestMessageBody == null) {
      throw new OrchestratorException(EXECUTOR_SERVICE_UNINITIALIZED);
    }
  }

  public void start() {
    checkInit();
    log.info(
        "Executing service:{}, env:{}",
        this.serviceRequestMessageBody.getServiceName(),
        this.serviceRequestMessageBody.getEnvironmentName());
    log.debug(
        "Executing service request message body : {}", this.serviceRequestMessageBody.toString());
    this.kubernetesRunnerProvisioner.provision(this.namespace);
    executeAndWait();
  }

  public void resume() {
    checkInit();
    log.info(
        "Resuming execution for service:{}, env:{}",
        this.serviceRequestMessageBody.getServiceName(),
        this.serviceRequestMessageBody.getEnvironmentName());
    log.debug(
        "Resuming execution for service request message body : {}",
        this.serviceRequestMessageBody.toString());
    // Update dag according to job status
    checkStateAndUpdateDag(new HashSet<>(this.serviceRequestMessageBody.getComponentActions()));
    // Resume rest of deploy
    executeAndWait();
  }

  @SneakyThrows
  void executeAndWait() {
    try (SharedIndexInformer<Job> informer =
        this.kubernetesRunnerProvisioner.watchJobStatusInNamespace(this)) {
      String serviceAction = "";
      informer.start();
      while (!this.dagBuilder.isEmpty()) {
        Set<ComponentAction> componentActionsToDeploy =
            this.dagBuilder.getNextComponentActionIds().stream()
                .map(
                    actionId ->
                        ServiceUtil.getComponentActionById(
                            actionId, this.serviceRequestMessageBody.getComponentActions()))
                .collect(Collectors.toSet());
        if (!componentActionsToDeploy.isEmpty()) {
          serviceAction = componentActionsToDeploy.stream().findFirst().get().getStage().getName();
        }
        if (!componentActionsToDeploy.isEmpty()) {
          doExecute(componentActionsToDeploy);
        }
        log.debug("Waiting for next execution");
        // Wait for 5 sec to check for new execution nodes
        Thread.sleep(Constants.EXECUTOR_WAIT_DURATION.toMillis());
      }
      // Stop watcher
      informer.stop();
      if (dagBuilder.executionFailure()) {
        // Send service execution failed message in queue
        this.sendServiceExecutionFailedMessage(deploymentId, serviceAction);
      } else if (dagBuilder.executionSuccess()) {
        // Send service execution success message in queue
        this.sendServiceExecutionSuccessMessage(deploymentId, serviceAction);
      }
    }
  }

  void doExecute(Set<ComponentAction> componentActions) {
    checkInit();
    for (ComponentAction componentAction : componentActions) {

      log.info("Executing component:{}", componentAction.getName());
      log.debug(
          "Component accounts:{}",
          Objects.requireNonNull(componentAction.getAccounts().toString()));
      log.debug("Component baseConfig:{}", componentAction.getBaseConfig().toString());
      log.debug("Component flavourConfig:{}", componentAction.getFlavourConfig().toString());
      if (componentAction.getOperationConfig() != null) {
        log.debug("Component operationConfig:{}", componentAction.getOperationConfig());
      }
      // TODO figure out deployment namespace
      String deploymentNamespace = this.serviceRequestMessageBody.getEnvironmentName();
      this.manifestService.init(
          ManifestServiceDto.builder()
              .environmentName(this.serviceRequestMessageBody.getEnvironmentName())
              .serviceName(this.serviceRequestMessageBody.getServiceName())
              .deploymentId(this.deploymentId)
              .componentAction(componentAction)
              .deploymentNamespace(deploymentNamespace)
              .orgId(this.serviceRequestMessageBody.getOrgId())
              .build());

      // Generate resource templates
      KubernetesListBuilder kubernetesListBuilder =
          new KubernetesListBuilder()
              .withItems(
                  this.manifestService.createConfigMap(),
                  this.manifestService.createSecret(),
                  this.manifestService.createServiceAccount(),
                  this.manifestService.createJob());

      // Create resources
      this.kubernetesRunnerProvisioner.applyManifests(kubernetesListBuilder.build(), namespace);
    }
  }

  public void updateJobStatus(
      String componentName,
      Integer componentActionId,
      String jobStatus,
      String stageName,
      String logs) {
    if (jobStatus.equals(Constants.JOB_SUCCESS)) {
      log.info("Component: {} execution succeeded", componentName);
      // Send component execution success message in queue
      this.sendComponentExecutionSuccessMessage(deploymentId, componentName, stageName);

      // Remove node from DAG
      dagBuilder.updateCompletedNode(componentActionId, TaskStatus.SUCCESSFUL);

    } else {
      log.info("Component: {} execution failed with error: {}", componentName, logs);
      // Send component execution failed message in queue
      this.sendComponentExecutionFailedMessage(deploymentId, componentName, stageName, logs);
      // Clear DAG to terminate execution if action is not validate
      if (stageName.equalsIgnoreCase(Constants.VALIDATE)) {
        dagBuilder.updateCompletedNode(componentActionId, TaskStatus.FAILED);
      } else {
        List<Integer> componentActionIds =
            dagBuilder.getDependentComponentActionIds(componentActionId);
        componentActionIds.forEach(
            id -> {
              Pair<String, String> componentNameAndActionName = getComponentNameAndActionName(id);
              this.sendComponentExecutionFailedMessage(
                  deploymentId,
                  componentNameAndActionName.getLeft(),
                  componentNameAndActionName.getRight(),
                  "Component execution failed due to dependent component failure");
            });
        dagBuilder.clearConnectedComponents(componentActionId);
      }
    }
  }

  void checkStateAndUpdateDag(Set<ComponentAction> componentActions) {
    for (ComponentAction componentAction : componentActions) {
      String manifestName =
          ManifestUtil.getManifestName(componentAction.getName(), componentAction.getId());
      // Skip componentAction if Job does not exists
      if (!kubernetesRunnerProvisioner.jobExists(manifestName, namespace)) {
        // If a job corresponding to an action does not exist, check and clean-up resources.
        // Resources could be present if job exited a long time ago.
        kubernetesRunnerProvisioner.deleteSecretIfExists(manifestName, namespace);
        kubernetesRunnerProvisioner.deleteConfigMapIfExists(manifestName, namespace);
        kubernetesRunnerProvisioner.deleteServiceAccountIfExists(manifestName, namespace);
        continue;
      }

      // If a job corresponding to an action is completed, remove it from the DAG
      if (kubernetesRunnerProvisioner.isJobSuccessful(manifestName, namespace)) {
        dagBuilder.setVisibilityFalse(componentAction.getId());
        dagBuilder.updateCompletedNode(componentAction.getId(), TaskStatus.SUCCESSFUL);
      } else if (kubernetesRunnerProvisioner.isJobFailed(manifestName, namespace)) {
        // If a job corresponding to an action is failed, delete corresponding resources and leave
        // it in the DAG
        kubernetesRunnerProvisioner.deleteSecretIfExists(manifestName, namespace);
        kubernetesRunnerProvisioner.deleteConfigMapIfExists(manifestName, namespace);
        kubernetesRunnerProvisioner.deleteServiceAccountIfExists(manifestName, namespace);
        kubernetesRunnerProvisioner.deleteJob(manifestName, namespace);
      } else if (kubernetesRunnerProvisioner.isJobRunning(manifestName, namespace)) {
        // If a job corresponding to an action is running, update its visibility to false
        dagBuilder.setVisibilityFalse(componentAction.getId());
      }
    }
  }

  public void handleDiscovery(
      String jobStatus, String componentName, String logs, String actionName)
      throws JsonProcessingException {
    if (jobStatus.equals(Constants.JOB_SUCCESS)) {
      discoveryClientService.handleDiscovery(
          serviceRequestMessageBody.getOrgId(),
          serviceRequestMessageBody.getComponentActions().stream()
              .filter(componentAction -> componentAction.getName().equals(componentName))
              .findFirst()
              .get(),
          logs,
          actionName.toUpperCase());
    }
  }

  @SneakyThrows
  public boolean checkDNSResolution(String componentName, String logs, String actionName) {
    // Skip DNS resolution for validate action
    if (!actionName.equalsIgnoreCase(Constants.DEPLOY_ACTION_NAME)
        && !actionName.equalsIgnoreCase(Constants.HEALTHCHECK_ACTION_NAME)) {
      return true;
    }
    String runnerDiscoveryOutput =
        discoveryClientService.getContentBetweenMarkers(
            logs, ODIN_DISCOVERY_MARKER_START, ODIN_DISCOVERY_MARKER_END);
    ComponentAction componentAction =
        serviceRequestMessageBody.getComponentActions().stream()
            .filter(
                action ->
                    action.getName().equals(componentName)
                        && action.getStage().getName().equalsIgnoreCase(actionName))
            .findFirst()
            .get();
    String discoveryFromUserData =
        discoveryClientService.getDiscoveryFromUserData(
            AppContext.getObjectMapper().writeValueAsString(componentAction.getBaseConfig()));

    if (discoveryFromUserData == null || discoveryFromUserData.isEmpty()) {
      log.info("DNS resolution not required for component: {}", componentName);
      return true;
    }
    // If user data discovery is present but runner discovery output is not present, throw error
    if (runnerDiscoveryOutput == null || runnerDiscoveryOutput.isEmpty()) {
      throw new OrchestratorException(RUNNER_DISCOVERY_OUTPUT_NOT_FOUND);
    }
    JsonNode runnerDiscoveryOutputNode =
        AppContext.getObjectMapper().readTree(runnerDiscoveryOutput);

    JsonNode userDataDiscoveryNode = AppContext.getObjectMapper().readTree(discoveryFromUserData);

    return discoveryClientService.doesDNSResolveCorrectly(
        runnerDiscoveryOutputNode, userDataDiscoveryNode);
  }

  public void cleanup() {
    kubernetesRunnerProvisioner.deleteNamespace(namespace);
  }

  private Pair<String, String> getComponentNameAndActionName(int componentActionId) {
    ComponentAction componentAction =
        serviceRequestMessageBody.getComponentActions().stream()
            .filter(action -> action.getId() == componentActionId)
            .findFirst()
            .orElseThrow();
    return Pair.of(componentAction.getName(), componentAction.getStage().getName());
  }

  @SneakyThrows
  public void sendComponentExecutionSuccessMessage(
      long deploymentId, String componentName, String stage) {
    ResponseMessage responseMessage =
        ResponseMessage.builder()
            .id(deploymentId)
            .executionId(AppContext.getTraceId())
            .type(ResponseMessageType.COMPONENT_STATUS)
            .status(TaskStatus.SUCCESSFUL)
            .data(ServiceResponseData.builder().stage(stage).componentName(componentName).build())
            .build();
    ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessage);
  }

  @SneakyThrows
  public void sendComponentExecutionFailedMessage(
      long deploymentId, String componentName, String stage, String logs) {
    ResponseMessage responseMessage =
        ResponseMessage.builder()
            .id(deploymentId)
            .executionId(AppContext.getTraceId())
            .type(ResponseMessageType.COMPONENT_STATUS)
            .status(TaskStatus.FAILED)
            .error(logs)
            .data(ServiceResponseData.builder().stage(stage).componentName(componentName).build())
            .build();
    ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessage);
  }

  @SneakyThrows
  public void sendServiceExecutionSuccessMessage(long deploymentId, String stage) {
    ResponseMessage responseMessage =
        ResponseMessage.builder()
            .id(deploymentId)
            .executionId(AppContext.getTraceId())
            .type(ResponseMessageType.SERVICE_STATUS)
            .status(TaskStatus.SUCCESSFUL)
            .data(ServiceResponseData.builder().stage(stage).build())
            .build();
    ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessage);
  }

  @SneakyThrows
  public void sendServiceExecutionFailedMessage(long deploymentId, String stage) {
    ResponseMessage responseMessage =
        ResponseMessage.builder()
            .id(deploymentId)
            .executionId(AppContext.getTraceId())
            .type(ResponseMessageType.SERVICE_STATUS)
            .status(TaskStatus.FAILED)
            .data(ServiceResponseData.builder().stage(stage).build())
            .build();
    ApplicationUtil.sendResponseMessage(this.messageProducer, responseMessage);
  }
}
