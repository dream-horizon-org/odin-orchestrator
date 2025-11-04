package com.dream11.orchestrator.provisioner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class OdinNamespaceProviderTest {

  final OdinNamespaceProvider odinNamespaceProvider = new OdinNamespaceProvider();

  @Test
  void testDeleteNullNamespaceError() {
    K8sServiceData.Cluster cluster = mock(K8sServiceData.Cluster.class);
    assertThatThrownBy(
            () -> this.odinNamespaceProvider.deleteNamespace("name", List.of(cluster), 0))
        .isInstanceOf(ExecutionException.class)
        .hasMessageContaining("java.lang.NullPointerException");
  }
}
