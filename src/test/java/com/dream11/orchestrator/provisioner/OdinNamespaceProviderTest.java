package com.dream11.orchestrator.provisioner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.dream11.orchestrator.dto.account.servicedata.K8sServiceData;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.concurrent.ExecutionException;


public class OdinNamespaceProviderTest {

    final OdinNamespaceProvider odinNamespaceProvider = new OdinNamespaceProvider();

    @Test
    void testDeleteNullNamespaceError()
    {
        K8sServiceData.Cluster cluster = mock(K8sServiceData.Cluster.class);
        assertThrows(
                ExecutionException.class,
                () -> this.odinNamespaceProvider.deleteNamespace("name",List.of(cluster),0),
                "Should have thrown ExecutionException");


    }
}
