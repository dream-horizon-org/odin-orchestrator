package com.dream11.orchestrator.processor;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dream11.orchestrator.dto.request.RequestMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class NamespaceMessageProcessorTest {

    @Mock NamespaceMessageProcessor namespaceMessageProcessor;
    @Mock RequestMessage nullRequestMessage;

    @Test
    void testProcessorWithNullRequestMessage() {
        assertThrows(
            NullPointerException.class,
            () -> this.namespaceMessageProcessor.process(this.nullRequestMessage),
            "Should have thrown NullPointerException");
    }

}
