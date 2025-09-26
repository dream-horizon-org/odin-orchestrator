package com.dream11.orchestrator.util;

import com.dream11.orchestrator.dto.ResponseMessage;
import com.dream11.orchestrator.inject.AppContext;
import com.dream11.queue.producer.MessageProducer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.Inflater;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ApplicationUtil {
  @SneakyThrows
  public <T> void runOnExecutorService(ExecutorService executorService, List<Callable<T>> tasks) {
    ExecutorCompletionService<T> executorCompletionService =
        new ExecutorCompletionService<>(executorService);
    List<Future<T>> results = tasks.stream().map(executorCompletionService::submit).toList();

    // Wait for futures to complete, throw exception if one of them fails.
    for (int i = 0; i < results.size(); i++) {
      try {
        executorCompletionService.take().get();
      } catch (Exception ex) {
        // Cancel all futures
        log.debug("Exception while executing tasks. Cancelling pending tasks.");
        results.forEach(result -> result.cancel(true));
        throw ex;
      }
    }
  }

  @SneakyThrows
  public static String decodeAndDecompress(String base64Data) {
    byte[] compressedData = Base64.getDecoder().decode(base64Data);
    Inflater inflater = new Inflater();
    inflater.setInput(compressedData);

    byte[] buffer = new byte[1024];
    int decompressedDataLength;

    // Decompress the data
    try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
      while (!inflater.finished()) {
        decompressedDataLength = inflater.inflate(buffer);
        outputStream.write(buffer, 0, decompressedDataLength);
      }
      String output = outputStream.toString(StandardCharsets.UTF_8);
      log.info("Decompressed data: {}", output);
      return output;
    } finally {
      inflater.end();
    }
  }

  public <T> void validate(T object) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<T>> constraintViolations = validator.validate(object);
    if (!constraintViolations.isEmpty()) {
      throw new ConstraintViolationException(constraintViolations);
    }
  }

  @SneakyThrows
  public void sendResponseMessage(
      MessageProducer<String> messageProducer, ResponseMessage responseMessage) {
    String jsonResponse = AppContext.getObjectMapper().writeValueAsString(responseMessage);
    log.info("Sending message: {}", jsonResponse);
    messageProducer.send(jsonResponse).get();
  }
}
