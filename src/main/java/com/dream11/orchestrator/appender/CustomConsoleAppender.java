package com.dream11.orchestrator.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.dream11.orchestrator.constants.Constants;
import com.dream11.orchestrator.inject.AppContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;

@Slf4j
public class CustomConsoleAppender extends ConsoleAppender<ILoggingEvent> {
  @Override
  protected void subAppend(ILoggingEvent event) {
    ((LoggingEvent) event)
        .addMarker(Markers.appendEntries(Map.of(Constants.TRACE_ID, AppContext.getTraceId())));
    super.subAppend(event);
  }
}
