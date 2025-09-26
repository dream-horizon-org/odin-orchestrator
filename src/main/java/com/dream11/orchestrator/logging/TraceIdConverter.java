package com.dream11.orchestrator.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.dream11.orchestrator.inject.AppContext;

public class TraceIdConverter extends ClassicConverter {
  @Override
  public String convert(ILoggingEvent event) {
    return AppContext.getTraceId();
  }
}
