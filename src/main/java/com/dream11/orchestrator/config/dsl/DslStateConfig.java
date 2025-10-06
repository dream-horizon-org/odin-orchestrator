package com.dream11.orchestrator.config.dsl;

import com.dream11.orchestrator.constants.DslStateProvider;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class DslStateConfig {
  @NotNull DslStateProvider provider;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "provider")
  @JsonSubTypes({@JsonSubTypes.Type(value = S3StateConfig.class, name = "S3")})
  @Valid
  @NotNull
  StateConfig config;

  @Data
  public static class S3StateConfig implements StateConfig {
    @NotBlank String bucket;
    @NotBlank String region;
    String endpoint = "";
    boolean forcePathStyle = false;

    @Override
    public Map<String, Object> getConfig(String key) {
      return Map.of(
          "uri",
          String.format("s3://%s/%s", this.bucket, key),
          "endpoint",
          this.endpoint,
          "region",
          this.region,
          "forcePathStyle",
          this.forcePathStyle);
    }
  }
}
