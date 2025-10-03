package com.dream11.orchestrator.service;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.MANIFEST_SERVICE_UNINITIALIZED;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.dto.ManifestServiceDto;
import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import com.dream11.orchestrator.dto.metadata.DslMetaData;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.util.AccountUtil;
import com.dream11.orchestrator.util.JsonUtil;
import com.dream11.orchestrator.util.ManifestUtil;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ManifestService {

  final AppConfig appConfig;

  private ManifestServiceDto manifestServiceDto;
  private String namespace;
  private String manifestName;

  public void init(ManifestServiceDto manifestServiceDto) {
    this.manifestServiceDto = manifestServiceDto;
    this.namespace =
        ManifestUtil.getNamespace(
            manifestServiceDto.getEnvironmentName(),
            manifestServiceDto.getServiceName(),
            manifestServiceDto.getDeploymentId());
    this.manifestName =
        ManifestUtil.getManifestName(
            manifestServiceDto.getComponentAction().getName(),
            manifestServiceDto.getComponentAction().getId());
  }

  private void checkInit() {
    if (this.manifestServiceDto == null) {
      throw new OrchestratorException(MANIFEST_SERVICE_UNINITIALIZED);
    }
  }

  private ObjectMeta buildObjectMeta(String name) {
    return this.buildObjectMeta(name, Map.of());
  }

  private ObjectMeta buildObjectMeta(String name, Map<String, String> annotations) {
    return new ObjectMetaBuilder()
        .withName(name)
        .withNamespace(this.namespace)
        .withLabels(this.getComponentLabels())
        .withAnnotations(annotations)
        .build();
  }

  private DslMetaData buildDslMetadata() {
    DslMetaData.DslMetaDataBuilder dslMetaDataBuilder =
        DslMetaData.builder()
            .flavour(this.manifestServiceDto.getComponentAction().getDeploymentType())
            .stateConfig(
                Map.of(
                    "provider",
                    this.appConfig.getDsl().getStateConfig().getProvider(),
                    "config",
                    this.appConfig
                        .getDsl()
                        .getStateConfig()
                        .getConfig()
                        .getConfig(
                            String.format(
                                "%s/%s/%s/%s.txt",
                                this.manifestServiceDto.getOrgId(),
                                this.manifestServiceDto.getEnvironmentName(),
                                this.manifestServiceDto.getServiceName(),
                                this.manifestServiceDto.getComponentAction().getName()))))
            .lockConfig(
                Map.of(
                    "provider",
                    this.appConfig.getDsl().getLockConfig().getProvider(),
                    "config",
                    this.appConfig
                        .getDsl()
                        .getLockConfig()
                        .getConfig()
                        .getConfig(
                            String.format(
                                "%s:%s:%s:%s",
                                manifestServiceDto.getOrgId(),
                                manifestServiceDto.getEnvironmentName(),
                                manifestServiceDto.getServiceName(),
                                manifestServiceDto.getComponentAction().getName()))))
            .stage(this.manifestServiceDto.getComponentAction().getStage().getName());
    if (this.manifestServiceDto.getComponentAction().getStage().getConfig() != null) {
      dslMetaDataBuilder.config(
          this.manifestServiceDto.getComponentAction().getStage().getConfig());
    }
    return dslMetaDataBuilder.build();
  }

  public ConfigMap createConfigMap() {
    checkInit();
    // Data
    Map<String, String> data =
        Map.of(
            "ODIN_COMPONENT_TYPE",
            this.manifestServiceDto.getComponentAction().getType(),
            "ODIN_COMPONENT_VERSION",
            this.manifestServiceDto.getComponentAction().getVersion());
    return new ConfigMapBuilder()
        .withMetadata(this.buildObjectMeta(this.manifestName))
        .withData(data)
        .build();
  }

  public Secret createSecret() {
    checkInit();
    // StringData
    Map<String, String> stringData =
        new HashMap<>(
            Map.ofEntries(
                Map.entry(
                    "ODIN_RUNNER_DIND_ENABLED",
                    String.valueOf(this.appConfig.getRunner().getDind().getEnabled())),
                Map.entry(
                    "ODIN_COMPONENT_REGISTRY_URL", this.appConfig.getComponentRegistry().getUrl()),
                Map.entry(
                    "ODIN_COMPONENT_REGISTRY_USERNAME",
                    this.appConfig.getComponentRegistry().getUsername()),
                Map.entry(
                    "ODIN_COMPONENT_REGISTRY_PASSWORD",
                    this.appConfig.getComponentRegistry().getPassword()),
                Map.entry("ODIN_DSL_URL", this.appConfig.getDsl().getUrl()),
                Map.entry("ODIN_DSL_USERNAME", this.appConfig.getDsl().getUsername()),
                Map.entry("ODIN_DSL_PASSWORD", this.appConfig.getDsl().getPassword()),
                Map.entry(
                    "ODIN_BASE_CONFIG",
                    JsonUtil.toJsonString(
                        this.manifestServiceDto.getComponentAction().getBaseConfig())),
                Map.entry(
                    "ODIN_FLAVOUR_CONFIG",
                    JsonUtil.toJsonString(
                        this.manifestServiceDto.getComponentAction().getFlavourConfig())),
                Map.entry(
                    "ODIN_COMPONENT_METADATA",
                    JsonUtil.toJsonString(
                        ManifestUtil.buildComponentMetaData(
                            this.manifestServiceDto.getComponentAction(),
                            this.manifestServiceDto.getEnvironmentName(),
                            this.manifestServiceDto.getDeploymentNamespace(),
                            this.manifestServiceDto.getOrgId(),
                            this.manifestServiceDto.getDeploymentId()))),
                Map.entry("ODIN_DSL_METADATA", JsonUtil.toJsonString(this.buildDslMetadata())),
                Map.entry(
                    "ODIN_CLOUD_PROVIDER",
                    this.manifestServiceDto
                        .getComponentAction()
                        .getAccounts()
                        .getAccount()
                        .getProvider()),
                Map.entry(
                    "ODIN_CLOUD_PROVIDER_DATA",
                    JsonUtil.toJsonString(
                        this.manifestServiceDto
                            .getComponentAction()
                            .getAccounts()
                            .getAccount()
                            .getAccountData()))));

    if (AccountUtil.hasServiceWithCategory(
        this.manifestServiceDto.getComponentAction().getAccounts().getAccount(),
        Constants.CATEGORY_KUBERNETES)) {
      K8sServiceData k8sServiceData =
          AccountUtil.getServiceWithCategory(
              this.manifestServiceDto.getComponentAction().getAccounts().getAccount().getServices(),
              Constants.CATEGORY_KUBERNETES,
              K8sServiceData.class);
      if (!k8sServiceData.getClusters().isEmpty()) {
        stringData.put(
            "ODIN_BASE64_ENCODED_KUBECONFIG",
            k8sServiceData.getClusters().get(0).getKubeconfig()); // Only one cluster is allowed
      }
    }

    if (this.manifestServiceDto.getComponentAction().getStage().getName().equals("operate")) {
      stringData.put(
          "ODIN_OPERATION_CONFIG",
          JsonUtil.toJsonString(this.manifestServiceDto.getComponentAction().getOperationConfig()));
    }

    return new SecretBuilder()
        .withMetadata(this.buildObjectMeta(this.manifestName))
        .withType("Opaque")
        .withStringData(stringData)
        .build();
  }

  public ServiceAccount createServiceAccount() {
    checkInit();
    return new ServiceAccountBuilder()
        .withMetadata(
            this.buildObjectMeta(
                this.manifestName,
                AccountUtil.getRunnerServiceAccountAnnotations(
                    this.manifestServiceDto.getComponentAction().getAccounts().getAccount())))
        .build();
  }

  public Job createJob() {
    checkInit();
    return new JobBuilder()
        .withMetadata(this.buildObjectMeta(this.manifestName))
        .withSpec(
            new JobSpecBuilder()
                .withTemplate(this.buildPodTemplateSpec())
                .withBackoffLimit(0)
                .build())
        .build();
  }

  private Map<String, String> getComponentLabels() {
    return Map.of(
        Constants.LABEL_COMPONENT_NAME,
        this.manifestServiceDto.getComponentAction().getName(),
        Constants.LABEL_COMPONENT_ACTION,
        this.manifestServiceDto.getComponentAction().getStage().getName(),
        Constants.LABEL_COMPONENT_ACTION_ID,
        this.manifestServiceDto.getComponentAction().getId().toString(),
        Constants.LABEL_SERVICE_NAME,
        this.manifestServiceDto.getServiceName(),
        Constants.LABEL_ENV_NAME,
        this.manifestServiceDto.getEnvironmentName());
  }

  private Map<String, String> getPodAnnotations() {
    return Map.of("kubectl.kubernetes.io/default-container", Constants.RUNNER);
  }

  private PodTemplateSpec buildPodTemplateSpec() {
    // Create container env vars
    List<EnvVar> containerEnvVars = this.createContainerEnvVars();

    // Create env from sources
    List<EnvFromSource> envFromSources = this.createEnvFromSources();
    // Containers
    List<Container> containers =
        new ArrayList<>(List.of(this.buildRunnerContainer(containerEnvVars, envFromSources)));
    if (this.appConfig.getRunner().getDind().getEnabled().equals(Boolean.TRUE)) {
      containers.add(this.buildDinDContainer(containerEnvVars, envFromSources));
    }

    // ImagePullSecret
    List<LocalObjectReference> imagePullSecrets =
        this.appConfig.getRunner().getDockerSecrets().stream()
            .map(
                dockerSecret ->
                    new LocalObjectReferenceBuilder().withName(dockerSecret.getName()).build())
            .toList();

    // Volumes
    List<Volume> volumes =
        new ArrayList<>(
            this.appConfig.getRunner().getHostVolumeMounts().stream()
                .map(
                    hostVolumeMount ->
                        new VolumeBuilder()
                            .withName(hostVolumeMount.getName())
                            .withHostPath(
                                new HostPathVolumeSourceBuilder()
                                    .withType(hostVolumeMount.getType())
                                    .withPath(hostVolumeMount.getHostPath())
                                    .build())
                            .build())
                .toList());

    if (this.appConfig.getRunner().getDind().getEnabled().equals(Boolean.TRUE)) {
      volumes.add(
          new VolumeBuilder()
              .withName(Constants.RUNNER_POD_VOLUME_NAME)
              .withEmptyDir(new EmptyDirVolumeSource())
              .build());
      volumes.add(
          new VolumeBuilder()
              .withName(Constants.SHARED_RUNNER_POD_VOLUME_NAME)
              .withEmptyDir(new EmptyDirVolumeSource())
              .build());
    }

    PodSpecBuilder podSpecBuilder =
        new PodSpecBuilder()
            .withImagePullSecrets(imagePullSecrets)
            .withServiceAccountName(this.manifestName)
            .withRestartPolicy("Never")
            .withShareProcessNamespace(true)
            .withContainers(containers)
            .withVolumes(volumes);

    ObjectMetaBuilder podSpecMetaBuilder =
        new ObjectMetaBuilder()
            .withLabels(this.getComponentLabels())
            .withAnnotations(this.getPodAnnotations());

    return new PodTemplateSpecBuilder()
        .withSpec(podSpecBuilder.build())
        .withMetadata(podSpecMetaBuilder.build())
        .build();
  }

  private List<VolumeMount> buildDindVolumeMounts() {
    return new ArrayList<>(
        List.of(
            new VolumeMountBuilder()
                .withName(Constants.RUNNER_POD_VOLUME_NAME)
                .withMountPath("/run")
                .build(),
            new VolumeMountBuilder()
                .withName(Constants.SHARED_RUNNER_POD_VOLUME_NAME)
                .withMountPath("/tmp")
                .build()));
  }

  private Container buildRunnerContainer(
      List<EnvVar> containerEnvVars, List<EnvFromSource> envFromSources) {
    List<VolumeMount> volumeMounts =
        new ArrayList<>(
            this.appConfig.getRunner().getHostVolumeMounts().stream()
                .map(
                    hostVolumeMount ->
                        new VolumeMountBuilder()
                            .withName(hostVolumeMount.getName())
                            .withMountPath(hostVolumeMount.getMountPath())
                            .build())
                .toList());

    if (this.appConfig.getRunner().getDind().getEnabled().equals(Boolean.TRUE)) {
      volumeMounts.addAll(this.buildDindVolumeMounts());
    }

    return new ContainerBuilder()
        .withName(Constants.RUNNER)
        .withImage(this.appConfig.getRunner().getImage())
        .withImagePullPolicy(this.appConfig.getRunner().getImagePullPolicy())
        .withEnv(containerEnvVars)
        .withEnvFrom(envFromSources)
        .withResources(
            new ResourceRequirementsBuilder()
                .withRequests(this.appConfig.getRunner().getResources().getRequests())
                .withLimits(this.appConfig.getRunner().getResources().getLimits())
                .build())
        .withVolumeMounts(volumeMounts)
        .build();
  }

  private Container buildDinDContainer(
      List<EnvVar> containerEnvVars, List<EnvFromSource> envFromSources) {
    return new ContainerBuilder()
        .withName(Constants.DIND)
        .withImage(this.appConfig.getRunner().getDind().getImage())
        .withImagePullPolicy(this.appConfig.getRunner().getDind().getImagePullPolicy())
        .withArgs("dockerd", "--host=unix:///run/docker.sock", "--group=$(DOCKER_GROUP_GID)")
        .withEnv(containerEnvVars)
        .addToEnv(new EnvVarBuilder().withName("DOCKER_GROUP_GID").withValue("1001").build())
        .withEnvFrom(envFromSources)
        .withResources(
            new ResourceRequirementsBuilder()
                .withRequests(this.appConfig.getRunner().getDind().getResources().getRequests())
                .withLimits(this.appConfig.getRunner().getDind().getResources().getLimits())
                .build())
        .withSecurityContext(new SecurityContextBuilder().withPrivileged(true).build())
        .withVolumeMounts(this.buildDindVolumeMounts())
        .build();
  }

  private List<EnvVar> createContainerEnvVars() {
    return this.appConfig.getRunner().getEnvVars().entrySet().stream()
        .map(
            entry ->
                new EnvVarBuilder().withName(entry.getKey()).withValue(entry.getValue()).build())
        .toList();
  }

  private List<EnvFromSource> createEnvFromSources() {
    // Add configMap env vars
    ConfigMapEnvSourceBuilder configMapEnvSourceBuilder =
        new ConfigMapEnvSourceBuilder().withName(this.manifestName);
    EnvFromSourceBuilder envFromConfigMapSourceBuilder =
        new EnvFromSourceBuilder().withConfigMapRef(configMapEnvSourceBuilder.build());

    // Add secret env vars
    SecretEnvSourceBuilder secretEnvSourceBuilder =
        new SecretEnvSourceBuilder().withName(this.manifestName);
    EnvFromSourceBuilder envFromSecretSourceBuilder =
        new EnvFromSourceBuilder().withSecretRef(secretEnvSourceBuilder.build());

    return List.of(envFromConfigMapSourceBuilder.build(), envFromSecretSourceBuilder.build());
  }
}
