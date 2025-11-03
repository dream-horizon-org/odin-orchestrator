package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.queue.QueueProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValueType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.SneakyThrows;
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
    assertThat(config.getRunner().getDind().getEnabled()).isTrue();
  }

  @Test
  void testReadConfigOverride() {
    // Arrange
    System.setProperty("runner.dind.enabled", "false");

    // Act
    AppConfig config = ConfigUtil.readConfig();
    // Assert
    assertThat(config).isNotNull();
    assertThat(config.getRunner().getDind().getEnabled()).isFalse();
  }

  @SneakyThrows
  private static Method parseArrayConfigMethod() {
    Method m = ConfigUtil.class.getDeclaredMethod("parseArrayConfig", Config.class, String.class);
    m.setAccessible(true);
    return m;
  }

  @SneakyThrows
  private static Config invokeParseArray(Config cfg, String path) {
    return (Config) parseArrayConfigMethod().invoke(null, cfg, path);
  }

  @Test
  void testParseArrayConfigParsesStringArrayToList() {
    String path = "runner.hostVolumeMounts";
    Config input =
        ConfigFactory.parseString(
            "runner.hostVolumeMounts = \"[\\\"/h1:/c1:ro\\\", \\\"/h2:/c2:rw\\\"]\"");

    assertThrows(
        InvocationTargetException.class,
        () -> invokeParseArray(input, ""),
        "Should have thrown InvocationTargetException");

    Config result = invokeParseArray(input, path);

    assertTrue(result.hasPath(path), "Result config should have the converted path");
    assertEquals(ConfigValueType.LIST, result.getValue(path).valueType(), "Expected a LIST value");

    ConfigList list = result.getList(path);
    assertEquals(2, list.size());
    assertEquals("/h1:/c1:ro", result.getStringList(path).get(0));
    assertEquals("/h2:/c2:rw", result.getStringList(path).get(1));

    Config merged = result.withFallback(input);
    assertEquals(ConfigValueType.LIST, merged.getValue(path).valueType());
  }
}
