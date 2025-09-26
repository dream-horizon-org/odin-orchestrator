package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DnsUtilTest {

  @Test
  void testIsDnsResolvableToTargetFailures() {

    assertThat(DnsUtils.isDnsResolvableToTarget("google.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtils.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtils.isDnsResolvableToTarget("test", Set.of())).isFalse();
  }
}
