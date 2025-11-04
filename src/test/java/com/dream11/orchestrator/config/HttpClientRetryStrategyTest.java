package com.dream11.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.IntStream;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.HttpStatusCode;

class HttpClientRetryStrategyTest {

  private final int retryCount = 3;
  private final int retryIntervalSecs = 7;
  private final HttpClientRetryStrategy strategy =
      new HttpClientRetryStrategy(retryCount, retryIntervalSecs);

  private static HttpResponse mockResponse(int status) {
    HttpResponse resp = mock(HttpResponse.class);
    when(resp.getCode()).thenReturn(status);
    return resp;
  }

  @Nested
  class RetryRequestOnException {

    @Test
    void trueWhileWithinLimit() {
      HttpRequest req = mock(HttpRequest.class);
      HttpContext ctx = mock(HttpContext.class);

      IntStream.rangeClosed(1, retryCount)
          .forEach(
              execCount -> {
                boolean retry = strategy.retryRequest(req, new IOException("boom"), execCount, ctx);
                assertThat(retry).isTrue();
              });
    }

    @Test
    void falseAfterLimit() {
      boolean retry = strategy.retryRequest(null, new IOException("boom"), retryCount + 1, null);
      assertThat(retry).isFalse();
    }

    @Test
    void ignoresNulls() {
      boolean retry = strategy.retryRequest(null, null, 1, null);
      assertThat(retry).isTrue();
    }
  }

  @Nested
  class RetryRequestOnResponseCode {

    @ParameterizedTest
    @ValueSource(
        ints = {
          HttpStatusCode.INTERNAL_SERVER_ERROR,
          HttpStatusCode.BAD_GATEWAY,
          HttpStatusCode.GATEWAY_TIMEOUT,
          HttpStatusCode.SERVICE_UNAVAILABLE,
          HttpStatusCode.REQUEST_TIMEOUT,
          HttpStatusCode.THROTTLING
        })
    void retryableStatusesTrueWithinLimit(int status) {
      HttpResponse resp = mockResponse(status);
      boolean retry = strategy.retryRequest(resp, retryCount, null);
      assertThat(retry).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        ints = {
          HttpStatusCode.INTERNAL_SERVER_ERROR,
          HttpStatusCode.BAD_GATEWAY,
          HttpStatusCode.GATEWAY_TIMEOUT,
          HttpStatusCode.SERVICE_UNAVAILABLE,
          HttpStatusCode.REQUEST_TIMEOUT,
          HttpStatusCode.THROTTLING
        })
    void retryableStatusesFalseAfterLimit(int status) {
      HttpResponse resp = mockResponse(status);
      boolean retry = strategy.retryRequest(resp, retryCount + 1, null);
      assertThat(retry).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 301, 304, 400, 401, 403, 404, 422})
    void nonRetryableStatusesFalse(int status) {
      HttpResponse resp = mockResponse(status);
      boolean retry = strategy.retryRequest(resp, 1, null);
      assertThat(retry).isFalse();
    }
  }

  @Test
  void retryInterval() {
    TimeValue tv = strategy.getRetryInterval(null, 1, null);
    assertThat(retryIntervalSecs).isEqualTo(tv.toSeconds());
    assertThat(TimeValue.ofSeconds(retryIntervalSecs)).isEqualTo(tv);
  }
}
