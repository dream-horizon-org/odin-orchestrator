package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DnsUtilTest {

  @Test
  void testIsDnsResolvableToTargetFailures() {

    assertThat(DnsUtil.isDnsResolvableToTarget("google.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0.1"))).isFalse();
    assertThat(DnsUtil.isDnsResolvableToTarget("test", Set.of())).isFalse();
  }
}
