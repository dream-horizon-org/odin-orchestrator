package com.dream11.orchestrator.provisioner;

import com.google.inject.Provider;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesClientProvider implements Provider<KubernetesClient> {
  private static KubernetesClient client;

  @Override
  public KubernetesClient get() {
    synchronized (KubernetesClientProvider.class) {
      if (client == null) {
        log.info("Creating new Kubernetes client");
        client = new KubernetesClientBuilder().build();
      }
    }
    return client;
  }
}
