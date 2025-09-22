package com.dream11.orchestrator.inject;

import com.dream11.orchestrator.client.HttpClientProvider;
import com.dream11.orchestrator.provisioner.KubernetesClientProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.hc.client5.http.classic.HttpClient;

public class MainModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(KubernetesClient.class).toProvider(KubernetesClientProvider.class).in(Singleton.class);
    bind(HttpClient.class).toProvider(HttpClientProvider.class).in(Singleton.class);
  }
}
