package com.dream11.orchestrator.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DnsUtilTest {

  @Test
  void testIsValidIPAddress() {
    assertTrue(DnsUtils.isValidIPAddress("127.0.0.1"));
    assertTrue(DnsUtils.isValidIPAddress("0.0.0.0"));
    assertTrue(DnsUtils.isValidIPAddress("255.255.255.255"));
    assertFalse(DnsUtils.isValidIPAddress("127.0.0.1.2"));
    assertFalse(DnsUtils.isValidIPAddress("abcd"));
    assertFalse(DnsUtils.isValidIPAddress("123.22.22.a"));
  }

  @Test
  void testIsDnsResolvableToTargetFailures() {

    assertFalse(DnsUtils.isDnsResolvableToTarget("google.com", Set.of("127.0.0.1")));
    assertFalse(DnsUtils.isDnsResolvableToTarget("dream11.com", Set.of("127.0.0.1")));
    assertFalse(DnsUtils.isDnsResolvableToTarget("test", Set.of()));
  }
}
