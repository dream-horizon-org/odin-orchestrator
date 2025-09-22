package com.dream11.orchestrator.client;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.config.HttpClientRetryStrategy;
import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class HttpClientProvider implements Provider<HttpClient> {

  private final AppConfig appConfig;

  @Override
  public HttpClient get() {
    // todo: Use a HttpClient which exposes these configs at request level
    return HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectionRequestTimeout(
                    Timeout.ofSeconds(this.appConfig.getDiscovery().getTimeoutSecs()))
                .setResponseTimeout(
                    Timeout.ofSeconds(this.appConfig.getDiscovery().getTimeoutSecs()))
                .build())
        .setRetryStrategy(
            new HttpClientRetryStrategy(
                this.appConfig.getDiscovery().getMaxRetries(),
                this.appConfig.getDiscovery().getRetryIntervalSecs()))
        .build();
  }
}
