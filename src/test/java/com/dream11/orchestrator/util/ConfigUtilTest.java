package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.queue.QueueProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConfigUtilTest {

  @AfterEach
  public void clearSystemProperties() {
    System.clearProperty("runner.dind.enabled");
  }

  @Test
  void testReadConfigSuccess() {
    // Arrange & Act
    AppConfig config = ConfigUtil.readConfig();
    // Assert
    assertThat(config).isNotNull();
    assertThat(config.getQueue().getRequest().getProvider()).isEqualTo(QueueProvider.SQS);
    assertThat(config.getRunner().getDind().getEnabled()).isFalse();
    // TODO add assertions
  }

  @Test
  void testReadConfigOverride() {
    // Arrange
    System.setProperty("runner.dind.enabled", "true");

    // Act
    AppConfig config = ConfigUtil.readConfig();
    // Assert
    assertThat(config).isNotNull();
    assertThat(config.getRunner().getDind().getEnabled()).isTrue();
  }
}
