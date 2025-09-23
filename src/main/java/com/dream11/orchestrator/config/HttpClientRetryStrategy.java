package com.dream11.orchestrator.config;

import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import software.amazon.awssdk.http.HttpStatusCode;

@RequiredArgsConstructor
public class HttpClientRetryStrategy implements HttpRequestRetryStrategy {

  private final int retryCount;
  private final int retryIntervalSecs;

  private final Set<Integer> retryableStatusCodes =
      Set.of(
          HttpStatusCode.INTERNAL_SERVER_ERROR,
          HttpStatusCode.BAD_GATEWAY,
          HttpStatusCode.GATEWAY_TIMEOUT,
          HttpStatusCode.SERVICE_UNAVAILABLE,
          HttpStatusCode.REQUEST_TIMEOUT,
          HttpStatusCode.THROTTLING);

  @Override
  public boolean retryRequest(
      HttpRequest request, IOException exception, int execCount, HttpContext context) {
    return execCount <= retryCount;
  }

  @Override
  public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
    return retryableStatusCodes.contains(response.getCode()) && execCount <= retryCount;
  }

  @Override
  public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
    return TimeValue.ofSeconds(retryIntervalSecs);
  }
}
