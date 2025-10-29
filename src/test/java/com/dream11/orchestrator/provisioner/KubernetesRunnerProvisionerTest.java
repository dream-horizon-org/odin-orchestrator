package com.dream11.orchestrator.provisioner;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;


public class KubernetesRunnerProvisionerTest {

    final KubernetesRunnerProvisioner kubernetesRunnerProvisioner = mock(KubernetesRunnerProvisioner.class);

    @Test
    void testDeleteNamespace () {
        this.kubernetesRunnerProvisioner.deleteNamespace("namespace");
    }

    @Test
    void testDeleteConfigMapIfExists() {
        this.kubernetesRunnerProvisioner.deleteConfigMapIfExists("configMapName","namespace");
    }
}
