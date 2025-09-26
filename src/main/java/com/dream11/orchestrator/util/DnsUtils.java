package com.dream11.orchestrator.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

@UtilityClass
@Slf4j
public class DnsUtils {

  @SneakyThrows
  public boolean isDnsResolvableToTarget(String dnsName, Set<String> targets) {

    if (targets.isEmpty()) {
      log.warn("No targets provided for verification for DNSname {}", dnsName);
      return false;
    }
    // remove trailing dot if target ends with a dot.
    targets =
        targets.stream()
            .map(target -> target.endsWith(".") ? target.substring(0, target.length() - 1) : target)
            .collect(Collectors.toSet());

    Lookup cnameLookup = new Lookup(dnsName, Type.CNAME);
    cnameLookup.setResolver(new SimpleResolver());
    Record[] cnameRecords = cnameLookup.run();

    Lookup aLookup = new Lookup(dnsName, Type.A);
    aLookup.setResolver(new SimpleResolver());
    Record[] aRecords = aLookup.run();

    Set<String> actualTargets = new HashSet<>();

    // Process CNAME records if lookup is successful
    if (cnameLookup.getResult() == Lookup.SUCCESSFUL) {
      Arrays.stream(cnameRecords)
          .forEach(
              dnsRecord -> {
                CNAMERecord cnameRecord = (CNAMERecord) dnsRecord;
                String fqdn = cnameRecord.getTarget().toString();
                actualTargets.add(fqdn.endsWith(".") ? fqdn.substring(0, fqdn.length() - 1) : fqdn);
              });
    }

    // Process A records if lookup is successful
    if (aLookup.getResult() == Lookup.SUCCESSFUL) {
      Arrays.stream(aRecords)
          .forEach(
              dnsRecord -> {
                ARecord aRecord = (ARecord) dnsRecord;
                actualTargets.add(aRecord.getAddress().getHostAddress());
              });
    }

    if (!actualTargets.containsAll(targets)) {
      log.error(
          "DNS lookup failed for {}: Expected targets: {} but got: {}",
          dnsName,
          targets,
          actualTargets);
      return false;
    }

    log.info("DNS lookup successful for {}: Resolved targets match: {}", dnsName, actualTargets);
    return true;
  }
}
