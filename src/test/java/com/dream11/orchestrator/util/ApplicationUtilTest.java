package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApplicationUtilTest {

  static ExecutorService EXECUTOR;

  @BeforeAll
  static void setup() {
    EXECUTOR = Executors.newFixedThreadPool(3);
  }

  @AfterAll
  static void tearDown() {
    if (EXECUTOR != null) {
      EXECUTOR.shutdownNow();
    }
  }

  @Test
  void testDecodeAndDecompress() {
    // Arrange
    String original = "Hello, Odin Orchestrator!";
    String encoded = TestUtil.compressAndEncode(original);

    // Act
    String result = ApplicationUtil.decodeAndDecompress(encoded);

    // Assert
    assertThat(result).isEqualTo(original);
  }

  @Test
  void testDecodeAndDecompressInvalidBase64() {
    // Arrange
    String invalidBase64 = "!@#not_base64";

    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.decodeAndDecompress(invalidBase64))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Illegal base64 character 21");
  }

  @Test
  void testDecodeAndDecompressNonCompressed() {
    // Arrange
    String plain = "this is not compressed";
    String base64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.decodeAndDecompress(base64))
        .isInstanceOf(DataFormatException.class)
        .hasMessage("incorrect header check");
  }

  @Test
  void testRunOnExecutorServiceSuccess() {
    // Arrange
    AtomicInteger counter = new AtomicInteger(0);
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      tasks.add(
          () -> {
            // Simulate small work
            counter.incrementAndGet();
            return 1;
          });
    }

    // Act
    ApplicationUtil.runOnExecutorService(EXECUTOR, tasks);

    // Assert
    assertThat(counter.get()).isEqualTo(5);
  }

  @Test
  void testRunOnExecutorServiceFailureCancelsPending() {
    // Arrange
    Callable<Void> longRunningTask =
        () -> {
          Thread.sleep(10000);
          return null;
        };

    Callable<Void> failingTask =
        () -> {
          throw new RuntimeException("exception");
        };

    // Act & Assert
    assertThatThrownBy(
            () ->
                ApplicationUtil.runOnExecutorService(
                    EXECUTOR, List.of(longRunningTask, failingTask)))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("exception");
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class SimpleBean {
    @NotBlank String childName;
  }

  @Test
  void testValidateThrowsException() {
    // Arrange
    SimpleBean bean = new SimpleBean();

    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.validate(bean))
        .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void testValidateSuccess() {
    // Arrange
    SimpleBean bean = new SimpleBean("child");

    // Act
    ApplicationUtil.validate(bean);
  }
}
