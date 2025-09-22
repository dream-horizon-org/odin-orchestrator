package com.dream11.orchestrator.provisioner;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;

class KubernetesClientProviderTest {

  @Test
  void testGetClientReturnsSameInstance() {
    // Arrange
    KubernetesClientProvider provider = new KubernetesClientProvider();

    // Act
    KubernetesClient firstClient = provider.get();
    KubernetesClient anotherClient = provider.get();

    // Assert
    assertThat(firstClient).isNotNull().isEqualTo(anotherClient);
  }
}
