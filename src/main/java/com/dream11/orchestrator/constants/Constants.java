package com.dream11.orchestrator.constants;

import io.fabric8.kubernetes.api.model.Quantity;
import java.time.Duration;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  // Categories
  public static final String CATEGORY_KUBERNETES = "KUBERNETES";
  public static final Duration EXECUTOR_WAIT_DURATION = Duration.ofSeconds(5);
  public static final String JOB_FAILED = "FAILED";
  // Kubernetes
  public static final String JOB_SUCCESS = "SUCCESS";
  public static final String LABEL_COMPONENT_ACTION = "componentAction";
  public static final String LABEL_COMPONENT_ACTION_ID = "componentActionId";
  public static final String LABEL_COMPONENT_NAME = "componentName";
  public static final String LABEL_ENV_NAME = "envName";
  public static final String LABEL_SERVICE_NAME = "serviceName";
  public static final String LABEL_CREATED_BY = "createdBy";
  public static final String LABEL_CREATED_AT = "createdAt";
  public static final String LABEL_TRACE_ID = "traceId";
  public static final int MESSAGE_TIMEOUT_SECONDS = 5;
  public static final String RUNNER_POD_VOLUME_NAME = "docker-sock";
  public static final String SHARED_RUNNER_POD_VOLUME_NAME = "shared";

  public static final String ODIN_DISCOVERY_MARKER_START =
      "------ODIN-DISCOVERY-MARKER-START------\n";
  public static final String ODIN_DISCOVERY_MARKER_END = "\n------ODIN-DISCOVERY-MARKER-END------";

  public static final String DISCOVERY_UPSERT_ACTION_NAME = "UPSERT";

  public static final String DISCOVERY_DELETE_ACTION_NAME = "DELETE";

  public static final String DEPLOY_ACTION_NAME = "DEPLOY";
  public static final String UNDEPLOY_ACTION_NAME = "UNDEPLOY";
  public static final String HEALTHCHECK_ACTION_NAME = "HEALTHCHECK";

  public static final String TRACE_ID = "TRACE_ID";
  public static final String RUNNER = "runner";
  public static final String DIND = "dind";
  public static final String ODIN = "odin";

  public static final Map<String, Quantity> RUNNER_CONTAINER_RESOURCE_REQUESTS =
      Map.of(
          "cpu", new Quantity("1000m"),
          "memory", new Quantity("100Mi"),
          "ephemeral-storage", new Quantity("1Gi"));

  public static final Map<String, Quantity> DIND_CONTAINER_RESOURCE_REQUESTS =
      Map.of(
          "cpu", new Quantity("500m"),
          "memory", new Quantity("100Mi"));
}
