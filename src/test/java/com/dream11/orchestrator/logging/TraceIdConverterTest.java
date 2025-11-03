package com.dream11.orchestrator.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.dream11.orchestrator.inject.AppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceIdConverterTest {

  private TraceIdConverter traceIdConverter;
  private ILoggingEvent mockLoggingEvent;

  @BeforeEach
  void setUp() {
    this.traceIdConverter = new TraceIdConverter();
    this.mockLoggingEvent = mock(ILoggingEvent.class);
    AppContext.setTraceId(null);
  }

  @AfterEach
  void tearDown() {
    AppContext.setTraceId(null);
  }

  @Test
  void testConvertWithValidTraceId() {
    // Arrange
    String expectedTraceId = "trace-id";
    AppContext.setTraceId(expectedTraceId);

    // Act
    String result = this.traceIdConverter.convert(this.mockLoggingEvent);

    // Assert
    assertThat(result).isEqualTo(expectedTraceId);
  }

  @Test
  void testConvertWithNoTraceId() {
    // Act
    String result = this.traceIdConverter.convert(this.mockLoggingEvent);

    // Assert
    assertThat(result).isEqualTo("na");
  }
}
