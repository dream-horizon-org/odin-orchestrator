package com.dream11.orchestrator.provisioner;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.HELM_COMMAND_EXECUTION_FAILED;

import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import com.dream11.orchestrator.dto.account.servicedata.OdinNamespaceProviderConfig;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.util.ApplicationUtil;
import com.dream11.orchestrator.util.CommandLineUtil;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class OdinNamespaceProvider implements NamespaceProvider {

  private final ExecutorService executorService =
      new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0L, TimeUnit.SECONDS, new SynchronousQueue<>());

  @Override
  public void createNamespace(
      String name, List<K8sServiceData.Cluster> clusters, Map<String, String> labels, long orgId) {
    ApplicationUtil.runOnExecutorService(
        this.executorService,
        clusters.stream()
            .map(
                cluster ->
                    (Callable<Void>)
                        () -> {
                          this.createNamespace(name, cluster, labels);
                          return null;
                        })
            .toList());
  }

  @Override
  public void deleteNamespace(String name, List<K8sServiceData.Cluster> clusters, long orgId) {
    ApplicationUtil.runOnExecutorService(
        this.executorService,
        clusters.stream()
            .map(
                cluster ->
                    (Callable<Void>)
                        () -> {
                          this.deleteNamespace(name, cluster);
                          return null;
                        })
            .toList());
  }

  @SneakyThrows
  private void createNamespace(
      String namespace, K8sServiceData.Cluster cluster, Map<String, String> labels) {
    // Create kubeconfig file
    log.info("Creating kubeconfig for cluster:[{}]", cluster.getName());
    this.createKubeconfig(cluster);
    OdinNamespaceProviderConfig namespaceConfig =
        ((OdinNamespaceProviderConfig) cluster.getNamespaceConfig().getProvider().getConfig());
    // Create namespace
    log.info("Creating/Updating namespace:[{}] in cluster:[{}]", namespace, cluster.getName());
    try (KubernetesClient kubernetesClient = this.createKubernetesClient(cluster.getName())) {
      kubernetesClient
          .namespaces()
          .resource(
              new NamespaceBuilder()
                  .withNewMetadata()
                  .withName(namespace)
                  .withLabels(labels)
                  .withAnnotations(namespaceConfig.getAnnotations())
                  .endMetadata()
                  .build())
          .serverSideApply();
    }
    // Install workloads in the namespace
    namespaceConfig
        .getWorkloads()
        .forEach(
            workLoad ->
                this.helmInstall(cluster.getName(), namespace, workLoad, cluster.getName()));
  }

  @SneakyThrows
  private void deleteNamespace(String namespace, K8sServiceData.Cluster cluster) {
    // Create kubeconfig file
    this.createKubeconfig(cluster);

    // Delete workloads in the namespace
    ((OdinNamespaceProviderConfig) cluster.getNamespaceConfig().getProvider().getConfig())
        .getWorkloads()
        .forEach(workLoad -> this.helmUninstall(namespace, workLoad, cluster.getName()));

    // Delete namespace
    log.info("Deleting namespace:[{}] in cluster:[{}]", namespace, cluster.getName());
    try (KubernetesClient kubernetesClient = this.createKubernetesClient(cluster.getName())) {
      kubernetesClient.namespaces().withName(namespace).delete();
    }
  }

  private void helmUninstall(
      String namespace, OdinNamespaceProviderConfig.WorkLoad workLoad, String kubeconfigPath) {
    // Uninstall helm chart
    log.info("Uninstalling helm chart:[{}] in cluster:[{}]", workLoad.getChart(), kubeconfigPath);
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute(
            "helm",
            "uninstall",
            workLoad.getChart(),
            "--namespace",
            namespace,
            "--kubeconfig",
            kubeconfigPath,
            "--wait");
    if (result.getExitCode() != 0) {
      if (result.getStdErr().contains("release: not found")) {
        log.warn("Helm release {}, not found. Skipping uninstallation", workLoad.getChart());
        return;
      }
      log.error(
          "Failed to uninstall helm chart:[{}] in cluster:[{}]",
          workLoad.getChart(),
          kubeconfigPath);
      throw new OrchestratorException(HELM_COMMAND_EXECUTION_FAILED, result.getStdErr());
    }
  }

  @SneakyThrows
  private void helmInstall(
      String clusterName,
      String namespace,
      OdinNamespaceProviderConfig.WorkLoad workLoad,
      String kubeconfigPath) {
    // Install the chart
    // Write values to a file
    String valuesFile = String.format("%s-%s-values.yaml", clusterName, workLoad.getChart());
    log.info(
        "Installing/Upgrading helm chart:[{}] in cluster:[{}]",
        workLoad.getChart(),
        kubeconfigPath);
    FileUtils.writeStringToFile(
        new File(valuesFile),
        AppContext.getObjectMapper().writeValueAsString(workLoad.getValues()),
        Charset.defaultCharset());
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute(
            "helm",
            "upgrade",
            "--install",
            workLoad.getChart(),
            workLoad.getChart(),
            "--version",
            workLoad.getVersion(),
            "--repo",
            workLoad.getRepo(),
            "--username",
            workLoad.getUsername(),
            "--password",
            workLoad.getPassword(),
            "--namespace",
            namespace,
            "--values",
            valuesFile,
            "--kubeconfig",
            kubeconfigPath,
            "--wait");
    if (result.getExitCode() != 0) {
      log.error(
          "Failed to install/upgrade helm chart:[{}] in cluster:[{}]",
          workLoad.getChart(),
          kubeconfigPath);
      throw new OrchestratorException(HELM_COMMAND_EXECUTION_FAILED, result.getStdErr());
    }
  }

  @SneakyThrows
  private void createKubeconfig(K8sServiceData.Cluster cluster) {
    FileUtils.writeStringToFile(
        new File(cluster.getName()),
        new String(Base64.getDecoder().decode(cluster.getKubeconfig())),
        Charset.defaultCharset());
  }

  @SneakyThrows
  private KubernetesClient createKubernetesClient(String kubeconfigPath) {
    return new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(Files.readString(Path.of(kubeconfigPath))))
        .build();
  }
}
