package com.dream11.orchestrator.provisioner;

import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import java.util.List;
import java.util.Map;

public interface NamespaceProvider {

  void createNamespace(
      String name, List<K8sServiceData.Cluster> clusters, Map<String, String> labels, long orgId);

  void deleteNamespace(String name, List<K8sServiceData.Cluster> clusters, long orgId);
}
