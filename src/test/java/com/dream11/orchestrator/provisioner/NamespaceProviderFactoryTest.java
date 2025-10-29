package com.dream11.orchestrator.provisioner;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.constants.NamespaceProviderType;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class NamespaceProviderFactoryTest {

    @Mock
    NamespaceProviderType nullNamespaceProviderType;

    @Test
    void testGetProviderNullArgument()
    {
        assertThrows(
            NullPointerException.class,
            () -> NamespaceProviderFactory.getProvider(this.nullNamespaceProviderType),
            "Should have thrown NullPointerException");
    }

    @Test
    void testGetProviderIllegalArgument()
    {
        assertThrows(
            IllegalArgumentException.class,
            () -> NamespaceProviderFactory.getProvider(NamespaceProviderType.valueOf("test")),
            "Should have thrown IllegalArgumentException");

    }
}
