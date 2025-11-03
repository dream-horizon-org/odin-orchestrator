package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class DnsUtilTest {

  @Mock Set<String> nullTargets;

  @Test
  void testIsDnsResolvableToTargetFailures() {

    assertThat(DnsUtil.isDnsResolvableToTarget("google.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0."))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("test", Set.of())).isFalse();

    assertThat(DnsUtil.isDnsResolvableToTarget("www.example.com.", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("www.example.", Set.of("127.0.0.1"))).isFalse();

    assertThrows(
        NullPointerException.class,
        () -> DnsUtil.isDnsResolvableToTarget("test", this.nullTargets),
        "Should have thrown NullPointerException");
  }
}
