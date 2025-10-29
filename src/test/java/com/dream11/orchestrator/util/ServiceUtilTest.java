package com.dream11.orchestrator.util;

import com.dream11.orchestrator.exception.OrchestratorException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceUtilTest {

    @Test
    void testGetComponentActionByIdWithEmptyInput()
    {
        assertThrows(
                OrchestratorException.class,
                () -> ServiceUtil.getComponentActionById(0, List.of()),
                "Should have thrown OrchestratorException");
    }
}
