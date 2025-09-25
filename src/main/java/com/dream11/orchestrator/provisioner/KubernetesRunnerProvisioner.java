package com.dream11.orchestrator.provisioner;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.NAMESPACE_CREATION_FAILED;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.WAITER_INTERRUPTED;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.config.RunnerConfig;
import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.service.ExecutorService;
import com.dream11.orchestrator.util.ManifestUtils;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class KubernetesRunnerProvisioner {
  final KubernetesClient k8sClient;

  final AppConfig appConfig;

  private void createNamespace(String namespace) {
    log.info("Creating namespace {}", namespace);
    this.k8sClient
        .namespaces()
        .resource(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .withLabels(
                    Map.of(
                        Constants.LABEL_CREATED_AT,
                        String.valueOf(System.currentTimeMillis()),
                        Constants.LABEL_CREATED_BY,
                        Constants.ODIN,
                        Constants.LABEL_TRACE_ID,
                        AppContext.getTraceId()))
                .endMetadata()
                .build())
        .create();
    waitForNamespaceReadiness(namespace, 30);
  }

  /**
   * Checks if a namespace is ready in given time
   *
   * @param namespace Name of deployment namespace
   * @param timeoutInSeconds secs to wait for namespace to be ready
   */
  public void waitForNamespaceReadiness(String namespace, int timeoutInSeconds) {
    int delayMs = 1000;
    while (timeoutInSeconds-- > 0) {
      Namespace nsObj = this.k8sClient.namespaces().withName(namespace).get();
      if (nsObj != null
          && nsObj.getStatus() != null
          && "Active".equalsIgnoreCase(nsObj.getStatus().getPhase())) {
        log.info("Namespace {} is Active", namespace);
        return;
      }
      try {
        Thread.sleep(delayMs);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new OrchestratorException(WAITER_INTERRUPTED, ie);
      }
    }
    throw new OrchestratorException(
        NAMESPACE_CREATION_FAILED,
        namespace,
        "Namespace did not reach Active phase within timeout");
  }

  /**
   * Checks if a namespace exists whose name is present in the list of namespaces matching without
   * traceId
   *
   * @param namespace Name of deployment namespace
   * @return boolean If exists or not
   */
  public boolean namespaceExists(String namespace) {
    NamespaceList namespaces = this.k8sClient.namespaces().list();
    String namespaceWithoutTraceId = namespace.substring(0, namespace.lastIndexOf("-"));
    return namespaces.getItems().stream()
        .anyMatch(n -> n.getMetadata().getName().contains(namespaceWithoutTraceId));
  }

  public void provision(String namespace) {
    // Prepare namespace for provision
    this.createNamespace(namespace);
    this.createDockerSecrets(this.appConfig.getRunner().getDockerSecrets(), namespace);
  }

  public void applyManifests(KubernetesList kubernetesList, String namespace) {
    log.info("Creating resources in namespace: {}", namespace);
    this.k8sClient.resourceList(kubernetesList).inNamespace(namespace).create();
  }

  private void createDockerSecrets(
      List<RunnerConfig.DockerSecret> dockerSecrets, String namespace) {
    log.info("Creating docker secrets for runner");
    for (RunnerConfig.DockerSecret dockerSecret : dockerSecrets) {
      Secret secret =
          new SecretBuilder()
              .withNewMetadata()
              .withName(dockerSecret.getName())
              .endMetadata()
              .withType("kubernetes.io/dockerconfigjson")
              .withData(
                  Map.of(
                      ".dockerconfigjson",
                      Base64.getEncoder()
                          .encodeToString(
                              String.format(
                                      "{\"auths\":{\"%s\":{\"username\":\"%s\",\"password\":\"%s\",\"auth\":\"%s\"}}}",
                                      dockerSecret.getServer(),
                                      dockerSecret.getUsername(),
                                      dockerSecret.getPassword(),
                                      Base64.getEncoder()
                                          .encodeToString(
                                              String.format(
                                                      "%s:%s",
                                                      dockerSecret.getUsername(),
                                                      dockerSecret.getPassword())
                                                  .getBytes()))
                                  .getBytes())))
              .build();
      this.k8sClient.secrets().inNamespace(namespace).resource(secret).create();
    }
  }

  // TODO
  protected String filterErrorLines(String log) {
    List<String> errorLines =
        Arrays.stream(log.split("\n")).filter(line -> line.contains("ERROR")).toList();

    int totalLines = errorLines.size();
    List<String> resultLines = new ArrayList<>();

    if (totalLines > 40) {
      resultLines.addAll(errorLines.subList(0, 20));
      resultLines.add("..."); // Indicate skipped lines
      resultLines.addAll(errorLines.subList(totalLines - 20, totalLines));
    } else {
      resultLines.addAll(errorLines);
    }

    return String.join("\n", resultLines);
  }

  public SharedIndexInformer<Job> watchJobStatusInNamespace(ExecutorService executorService) {
    return this.k8sClient
        .batch()
        .v1()
        .jobs()
        .inNamespace(executorService.getNamespace())
        .inform(
            new ResourceEventHandler<>() {
              @Override
              public void onAdd(Job job) {
                log.info(
                    "Job [{}] added successfully",
                    job.getMetadata().getLabels().get(Constants.LABEL_COMPONENT_NAME));
              }

              @Override
              public void onUpdate(Job oldJob, Job newJob) {
                // Only process job success or failed events
                if (newJob.getStatus().getSucceeded() == null
                    && newJob.getStatus().getFailed() == null) {
                  return;
                }
                String componentName =
                    newJob.getMetadata().getLabels().get(Constants.LABEL_COMPONENT_NAME);
                String actionName =
                    newJob.getMetadata().getLabels().get(Constants.LABEL_COMPONENT_ACTION);
                int componentActionId =
                    Integer.parseInt(
                        newJob.getMetadata().getLabels().get(Constants.LABEL_COMPONENT_ACTION_ID));
                String status =
                    newJob.getStatus().getSucceeded() != null
                        ? Constants.JOB_SUCCESS
                        : Constants.JOB_FAILED;

                String logs =
                    String.valueOf(
                        k8sClient
                            .pods()
                            .inNamespace(executorService.getNamespace())
                            .withLabelSelector(
                                new LabelSelectorBuilder()
                                    .withMatchLabels(
                                        Map.of(
                                            "job-name",
                                            ManifestUtils.getManifestName(
                                                componentName, componentActionId)))
                                    .build())
                            .resources()
                            .map(pod -> pod.inContainer(Constants.RUNNER).getLog())
                            .toList());

                log.info("Job [{}] event received with status:{}", componentName, status);
                log.info("Logs for job [{}]: {}", componentName, logs);
                try {
                  executorService.handleDiscovery(status, componentName, logs, actionName);
                  if (status.equals(Constants.JOB_SUCCESS)
                      && (!executorService.checkDNSResolution(componentName, logs, actionName))) {
                    status = Constants.JOB_FAILED;
                  }
                  // send only error logs
                  logs = filterErrorLines(logs);
                  executorService.updateJobStatus(
                      componentName, componentActionId, status, actionName, logs);
                } catch (Exception e) {
                  log.error(
                      "Error while handling discovery for component [{}], message: {}, cause: {}",
                      componentName,
                      e.getMessage(),
                      e.getCause());
                  // send only error logs
                  logs = filterErrorLines(logs);
                  executorService.updateJobStatus(
                      componentName,
                      componentActionId,
                      Constants.JOB_FAILED,
                      actionName,
                      logs + "\n" + e.getMessage());
                }
              }

              @Override
              public void onDelete(Job job, boolean b) {
                log.info(
                    "Job [{}] deleted successfully",
                    job.getMetadata().getLabels().get(Constants.LABEL_COMPONENT_NAME));
              }
            });
  }

  public void deleteNamespace(String namespace) {
    log.info("Deleting namespace {}", namespace);
    this.k8sClient.namespaces().withName(namespace).delete();
  }

  // ConfigMap Utils
  public void deleteConfigMapIfExists(String configMapName, String namespace) {
    this.k8sClient.configMaps().inNamespace(namespace).withName(configMapName).delete();
  }

  // Secret Utils
  public void deleteSecretIfExists(String secretName, String namespace) {
    this.k8sClient.secrets().inNamespace(namespace).withName(secretName).delete();
  }

  // ServiceAccount Utils
  public void deleteServiceAccountIfExists(String serviceAccountName, String namespace) {
    this.k8sClient.serviceAccounts().inNamespace(namespace).withName(serviceAccountName).delete();
  }

  // Job Utils
  public boolean jobExists(String jobName, String namespace) {
    return this.k8sClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).get()
        != null;
  }

  public boolean isJobRunning(String jobName, String namespace) {
    JobStatus status = this.getJobStatus(jobName, namespace);
    return status.getActive() != null && status.getActive().equals(1);
  }

  public boolean isJobSuccessful(String jobName, String namespace) {
    JobStatus status = this.getJobStatus(jobName, namespace);
    return status.getSucceeded() != null && status.getSucceeded().equals(1);
  }

  public boolean isJobFailed(String jobName, String namespace) {
    JobStatus status = this.getJobStatus(jobName, namespace);
    return status.getFailed() != null && status.getFailed().equals(1);
  }

  private JobStatus getJobStatus(String jobName, String namespace) {
    return this.k8sClient
        .batch()
        .v1()
        .jobs()
        .inNamespace(namespace)
        .withName(jobName)
        .require()
        .getStatus();
  }

  public void deleteJob(String jobName, String namespace) {
    this.k8sClient.batch().v1().jobs().inNamespace(namespace).withName(jobName).delete();
  }
}
