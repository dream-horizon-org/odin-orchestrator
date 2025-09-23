package com.dream11.orchestrator.util;

import com.dream11.orchestrator.dto.constants.RequestMessageType;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.Deflater;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
@UtilityClass
public class TestUtil {

  public JSONObject getComponentAction(String name, String action, String accounts) {
    return TestUtil.getComponentAction(name, action, accounts, false);
  }

  public JSONObject getComponentAction(String name, String action, String accounts, boolean error) {
    return new JSONObject(
        """
            {
              "id": %d,
              "name": "%s",
              "type": "example",
              "version": "0.0.1",
              "deploymentType": "aws_flavour",
              "provider": "LOCAL",
              "dependsOn": [],
              "stage": {
                "name": "%s",
                "config": {
                  "stageName" : "deploy"
                }
              },
              "baseConfig": {},
              "flavourConfig": {
                "error": %s
               },
              "operationConfig": {},
              "accounts":%s
            }
            """
            .formatted(TestUtil.generateIntegerUUID(), name, action, error, accounts));
  }

  @SneakyThrows
  public String getAccountData() {
    return TestUtil.getAccountData(Map.of());
  }

  @SneakyThrows
  public String getAccountData(Map<String, Object> dataModel) {
    String templatisedAccountData =
        """
        {
          "account": {
            "services": [
            {
              "name" : "EKS",
              "category" : "KUBERNETES",
              "data" : {
                "clusters" : [
                  {
                    "name": "dev",
                    "kubeconfig": "${BASE64_ENCODED_KUBECONFIG!''}",
                    "namespaceConfig": {
                      "provider": {
                        "name": "${NAMESPACE_PROVIDER!'ODIN'}",
                        "config": {
                          "annotations": {
                            "annotationKey": "annotationValue"
                          },
                          "workloads": [
                            {
                              "repo": "https://helm.github.io/examples",
                              "chart": "hello-world",
                              "version": "${CHART_VERSION!'0.1.0'}",
                              "username": "",
                              "password": "",
                              "values": {}
                            }
                          ]
                        }
                      }
                    }
                  }
                ]
              }
            }

            ],
            "data": {
              "resourceLabels": {
                "provisioned-by-user" : "odin"
              },
              "runnerServiceAccountAnnotations": {
                "annotationKey": "annotationValue"
              }
            },
            "provider": "LOCAL",
            "category": "CLOUD",
            "name": "test"
          }
        }
        """;
    return FreemarkerUtil.substituteValues("", templatisedAccountData, dataModel);
  }

  public String getRandomString() {
    return String.format("orch-test-%d", generateIntegerUUID());
  }

  public JSONObject getServiceMessageBody(
      List<JSONObject> componentActions, String envName, String serviceName) {
    return new JSONObject()
        .put("environmentName", envName)
        .put("serviceName", serviceName)
        .put("componentActions", componentActions);
  }

  public JSONObject getNamespaceMessageBody(String namespace, String action, String accountData) {
    return new JSONObject()
        .put("name", namespace)
        .put("action", action)
        .put("account", new JSONObject(accountData).get("account"));
  }

  public JSONObject getServiceRequestMessage(JSONObject messageBody) {
    return TestUtil.getServiceRequestMessage(messageBody, generateIntegerUUID());
  }

  public JSONObject getServiceRequestMessage(JSONObject messageBody, long taskId) {
    return TestUtil.getRequestMessage(messageBody, taskId, RequestMessageType.SERVICE);
  }

  public JSONObject getNamespaceRequestMessage(JSONObject messageBody, long taskId) {
    return TestUtil.getRequestMessage(messageBody, taskId, RequestMessageType.NAMESPACE);
  }

  public JSONObject getRequestMessage(
      JSONObject messageBody, long taskId, RequestMessageType requestMessageType) {
    return new JSONObject()
        .put("type", requestMessageType.toString())
        .put("id", taskId)
        .put("body", messageBody)
        .put("traceId", TestUtil.getRandomString());
  }

  public int generateIntegerUUID() {
    SecureRandom random = new SecureRandom(); // Compliant for security-sensitive use cases
    return random.nextInt(1000, 10000);
  }

  @SneakyThrows
  public KubernetesClient createKubernetesClient(String kubeConfigFilePath) {
    return new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(Files.readString(Path.of(kubeConfigFilePath))))
        .build();
  }

  public void deleteNamespace(KubernetesClient kubernetesClient, String namespace) {
    kubernetesClient.namespaces().withName(namespace).delete();
  }

  public void createNamespace(KubernetesClient kubernetesClient, String namespace) {
    kubernetesClient
        .namespaces()
        .resource(
            new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build())
        .create();
  }

  public Optional<Namespace> getNamespace(KubernetesClient kubernetesClient, String name) {
    Namespace namespace = kubernetesClient.namespaces().withName(name).get();
    return Objects.nonNull(namespace) ? Optional.of(namespace) : Optional.empty();
  }

  public Optional<Deployment> getDeployment(
      KubernetesClient kubernetesClient, String name, String namespace) {
    Deployment deployment =
        kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).get();
    return Objects.nonNull(deployment) ? Optional.of(deployment) : Optional.empty();
  }

  public String compressAndEncode(String data) {
    try {
      byte[] inputBytes = data.getBytes(StandardCharsets.UTF_8);

      Deflater deflater = new Deflater();
      deflater.setInput(inputBytes);
      deflater.finish();

      byte[] buffer = new byte[1024];
      int compressedDataLength;

      try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
        while (!deflater.finished()) {
          compressedDataLength = deflater.deflate(buffer);
          outputStream.write(buffer, 0, compressedDataLength);
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
      } finally {
        deflater.end();
      }
    } catch (Exception e) {
      log.error("Error while compressing data {}", e.getMessage(), e);
      return data;
    }
  }
}
