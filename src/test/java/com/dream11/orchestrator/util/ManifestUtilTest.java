package com.dream11.orchestrator.util;

import com.dream11.orchestrator.inject.AppContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ManifestUtilTest {

    @BeforeEach
    void setUp() {
        AppContext.setTraceId("test-trace");
    }

    @AfterEach
    void tearDown() {
        AppContext.setTraceId(null);
    }

    @ParameterizedTest(name = "env=\"{0}\", service=\"{1}\", id={2} ➜ \"{3}\"")
    @CsvSource({
            "envName, serviceName, 0, envName-serviceName-0-test",
            "prod, api, 42, prod-api-42-test",
            "staging, payments, 123456789, staging-payments-123456789-test",
            "dev, svc, -1, dev-svc--1-test"
    })
    void buildsExpectedNamespace(String env, String service, long componentExecutionId, String expected) {
        String actual = ManifestUtil.getNamespace(env, service, componentExecutionId);
        assertEquals(expected, actual);
    }

    @Test
    void isSameForSameInputs() {
        String a = ManifestUtil.getNamespace("envName", "serviceName", 0L);
        String b = ManifestUtil.getNamespace("envName", "serviceName", 0L);
        assertEquals(a, b);

    }
}
