package com.dream11.orchestrator.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComponentRegistryConfig {
  @NotBlank String url;
  @NotNull String username = "";
  @NotNull String password = "";

  @AssertTrue(message = "Either both or none username password must be set")
  boolean isValidUsernamePassword() {
    return (this.username.isEmpty() && this.password.isEmpty())
        || (!this.username.isEmpty() && !this.password.isEmpty());
  }
}
