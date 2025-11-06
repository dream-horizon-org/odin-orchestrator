package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DnsUtilTest {

  @Test
  void testIsDnsResolvableToTargetFailures() {

    assertThat(DnsUtil.isDnsResolvableToTarget("google.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0."))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("test", Set.of())).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("www.example.com.", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("www.example.", Set.of("127.0.0.1"))).isFalse();
  }

  @Test
  void testIsDnsResolvableToTargetExceptionCase() {
    // Assert
    assertThatThrownBy(() -> DnsUtil.isDnsResolvableToTarget("test", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining(
            "Cannot invoke \"java.util.Set.isEmpty()\" because \"targets\" is null");
  }
}
