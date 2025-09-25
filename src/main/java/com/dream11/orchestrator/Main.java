package com.dream11.orchestrator;

import com.dream11.orchestrator.inject.AppContext;
import com.dream11.orchestrator.inject.ConfigModule;
import com.dream11.orchestrator.inject.MainModule;
import com.dream11.orchestrator.util.ConfigUtils;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class Main {
  public void main(String[] args) {
    log.info("Starting odin orchestrator");
    AppContext.initialize(
        List.of(new MainModule(), ConfigModule.builder().config(ConfigUtils.readConfig()).build()));
    Orchestrator orchestrator = AppContext.getInstance(Orchestrator.class);
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }
}
