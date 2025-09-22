package com.dream11.orchestrator.provisioner;

import com.dream11.orchestrator.constants.NamespaceProviderType;
import com.dream11.orchestrator.inject.AppContext;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NamespaceProviderFactory {

  public NamespaceProvider getProvider(@NonNull NamespaceProviderType namespaceProviderType) {
    if (namespaceProviderType == NamespaceProviderType.ODIN) {
      return AppContext.getInstance(OdinNamespaceProvider.class);
    }
    throw new IllegalArgumentException(
        String.format("Provider %s not supported", namespaceProviderType));
  }
}
