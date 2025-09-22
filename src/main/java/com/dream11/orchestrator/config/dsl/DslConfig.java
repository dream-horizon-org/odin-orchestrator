package com.dream11.orchestrator.config.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DslConfig {
  @NotBlank String url;
  @NotNull String username = "";
  @NotNull String password = "";

  @JsonProperty("state")
  @NotNull
  @Valid
  DslStateConfig stateConfig;

  @JsonProperty("lock")
  @NotNull
  @Valid
  DslLockConfig lockConfig;

  @AssertTrue(message = "Either both or none username password must be set")
  boolean isValidUsernamePassword() {
    return (this.username.isEmpty() && this.password.isEmpty())
        || (!this.username.isEmpty() && !this.password.isEmpty());
  }
}
