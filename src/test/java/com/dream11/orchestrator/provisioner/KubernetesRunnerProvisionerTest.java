package com.dream11.orchestrator.provisioner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.util.TestUtil;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Base64;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KubernetesRunnerProvisionerTest {

  final String namespace = "namespace-0";
  static final String KUBECONFIG_PATH = "kubeconfig.yaml";
  static final String BASE64_ENCODED_KUBECONFIG = "BASE64_ENCODED_KUBECONFIG";

  static KubernetesClient k8sClient;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  AppConfig appConfig;

  KubernetesRunnerProvisioner kubernetesRunnerProvisioner;

  @BeforeEach
  void setUp() {
    createKubeconfig();
    k8sClient = TestUtil.createKubernetesClient(KUBECONFIG_PATH);
    kubernetesRunnerProvisioner = new KubernetesRunnerProvisioner(k8sClient, appConfig);
    if (!this.kubernetesRunnerProvisioner.namespaceExists(namespace))
      this.kubernetesRunnerProvisioner.provision(namespace);
  }

  @AfterEach
  void reset() {
    if (this.kubernetesRunnerProvisioner.namespaceExists(namespace))
      this.kubernetesRunnerProvisioner.deleteNamespace(namespace);
  }

  @Test
  void testWaitForNamespaceReadinessWithWrongNamespace() {
    assertThrows(
        OrchestratorException.class,
        () -> this.kubernetesRunnerProvisioner.waitForNamespaceReadiness("wrong-namespace-0", 2));
  }

  @Test
  void testWaitForNamespaceReadinessWithNullStatus() {

    Namespace nsObj = k8sClient.namespaces().withName(namespace).get();
    nsObj.setStatus(null);
    assertThrows(
        OrchestratorException.class,
        () -> this.kubernetesRunnerProvisioner.waitForNamespaceReadiness(this.namespace, 2));
  }

  @Test
  void testFilterErrorLinesWithLongLog() {
    String long_log = "This is a ERROR test line\n".repeat(41);
    String short_log = "This is a ERROR test line\n".repeat(20);

    String long_filtered_log = this.kubernetesRunnerProvisioner.filterErrorLines(long_log);
    String short_filtered_log = this.kubernetesRunnerProvisioner.filterErrorLines(short_log);

    assertEquals(long_filtered_log, short_filtered_log + "\n...\n" + short_filtered_log);
  }

  @Test
  void testNamespaceExists() {
    assertTrue(this.kubernetesRunnerProvisioner.namespaceExists(namespace));
  }

  @Test
  void testDeleteConfigMapIfExists() {
    this.kubernetesRunnerProvisioner.deleteConfigMapIfExists("configmap-0", namespace);
  }

  @Test
  void testDeleteSecretIfExists() {
    this.kubernetesRunnerProvisioner.deleteSecretIfExists("secret-name", namespace);
  }

  @Test
  void testDeleteServiceAccountIfExists() {
    this.kubernetesRunnerProvisioner.deleteServiceAccountIfExists("account-name", namespace);
  }

  @Test
  void testFalseJob() {
    this.kubernetesRunnerProvisioner.deleteJob("false-job", namespace);
    assertThat(this.kubernetesRunnerProvisioner.jobExists("false-job", namespace)).isFalse();
  }

  @SneakyThrows
  static void createKubeconfig() {
    FileUtils.writeStringToFile(
        new File(KUBECONFIG_PATH),
        new String(Base64.getDecoder().decode(System.getenv(BASE64_ENCODED_KUBECONFIG))),
        Charset.defaultCharset());
  }
}
