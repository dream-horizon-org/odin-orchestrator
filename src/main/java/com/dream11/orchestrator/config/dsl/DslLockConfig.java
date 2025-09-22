package com.dream11.orchestrator.config.dsl;

import com.dream11.orchestrator.constants.DslLockProvider;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class DslLockConfig {
  @NotNull DslLockProvider provider;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "provider")
  @JsonSubTypes({@JsonSubTypes.Type(value = RedisLockConfig.class, name = "redis")})
  @Valid
  @NotNull
  LockConfig config;

  @Data
  public static class RedisLockConfig implements LockConfig {
    @NotBlank String host;
    @NotNull Integer port = 6379;

    @Override
    public Map<String, Object> getConfig(String key) {
      return Map.of("key", key, "host", this.host, "port", this.port);
    }
  }
}
