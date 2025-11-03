package com.dream11.orchestrator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ComponentRegistryConfigTest {

  @SuppressWarnings("resource")
  @Test
  void testIsValidUsernamePassword() {
    ComponentRegistryConfig componentRegistryConfig = new ComponentRegistryConfig();
    componentRegistryConfig.setUrl("url");
    componentRegistryConfig.setUsername("user");
    componentRegistryConfig.setPassword("password");

    Validator v = Validation.buildDefaultValidatorFactory().getValidator();

    Set<ConstraintViolation<ComponentRegistryConfig>> violations =
        v.validate(componentRegistryConfig);
    assertEquals(0, violations.size());

    componentRegistryConfig.setPassword("");
    violations = v.validate(componentRegistryConfig);
    assertEquals(1, violations.size());

    componentRegistryConfig.setUsername("");
    violations = v.validate(componentRegistryConfig);
    assertEquals(0, violations.size());

    componentRegistryConfig.setPassword("password");
    violations = v.validate(componentRegistryConfig);
    assertEquals(1, violations.size());
  }
}
