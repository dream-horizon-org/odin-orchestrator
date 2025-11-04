package com.dream11.orchestrator.provisioner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.constants.NamespaceProviderType;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NamespaceProviderFactoryTest {

  @Mock NamespaceProviderType nullNamespaceProviderType;

  @Test
  void testGetProviderNullArgument() {
    assertThatThrownBy(() -> NamespaceProviderFactory.getProvider(this.nullNamespaceProviderType))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("namespaceProviderType is marked non-null but is null");
  }

  @Test
  void testGetProviderIllegalArgument() {
    assertThatThrownBy(
            () -> NamespaceProviderFactory.getProvider(NamespaceProviderType.valueOf("test")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "No enum constant com.dream11.orchestrator.constants.NamespaceProviderType.test");
  }
}
