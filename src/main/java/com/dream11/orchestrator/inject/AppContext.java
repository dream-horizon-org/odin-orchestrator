package com.dream11.orchestrator.inject;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.APP_CONTEXT_ALREADY_INITIALIZED;
import static com.dream11.orchestrator.exception.OrchestratorExceptionType.APP_CONTEXT_UNINITIALIZED;

import com.dream11.orchestrator.VisibleForTesting;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import lombok.Getter;

public class AppContext {
  private static AppContext contextInstance = null;
  private static String traceId;

  @Getter
  private static final ObjectMapper objectMapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .build()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final Injector injector;

  private AppContext(List<Module> modules) {
    injector = Guice.createInjector(modules);
  }

  public static synchronized void initialize(List<Module> modules) {
    if (contextInstance != null) {
      throw new OrchestratorException(APP_CONTEXT_ALREADY_INITIALIZED);
    } else {
      contextInstance = new AppContext(modules);
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NOT_REQUIRED)
  public static synchronized void reset() {
    contextInstance = null;
  }

  private static AppContext instance() {
    if (contextInstance != null) {
      return contextInstance;
    }
    throw new OrchestratorException(APP_CONTEXT_UNINITIALIZED);
  }

  public static <T> T getInstance(Class<T> klazz) {
    return instance().injector.getInstance(klazz);
  }

  public static void setTraceId(String traceId) {
    AppContext.traceId = traceId;
  }

  public static String getTraceId() {
    return AppContext.traceId != null ? AppContext.traceId : "na";
  }
}
