package com.dream11.orchestrator.util;

import com.dream11.orchestrator.config.AppConfig;
import com.dream11.orchestrator.inject.AppContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class ConfigUtil {
  @SneakyThrows
  public AppConfig readConfig() {
    ConfigFactory.invalidateCaches();
    Config config =
        ConfigFactory.load("application.conf")
            .withFallback(ConfigFactory.load("application-default.conf"))
            .resolve();
    config = ConfigUtil.parseArrayConfig(config, "runner.hostVolumeMounts").withFallback(config);
    config = ConfigUtil.parseArrayConfig(config, "runner.dockerSecrets").withFallback(config);

    AppConfig appConfig =
        AppContext.getObjectMapper().convertValue(config.root().unwrapped(), AppConfig.class);
    log.debug("Loaded config: {}", appConfig);
    ApplicationUtil.validate(appConfig);
    return appConfig;
  }

  private Config parseArrayConfig(Config config, String path) {
    if (config.getValue(path).valueType() == ConfigValueType.STRING) {
      return ConfigFactory.parseString(path + "=" + config.getString(path));
    }
    return config;
  }
}
