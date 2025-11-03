package com.dream11.orchestrator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dream11.orchestrator.config.dsl.DslConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DslConfigTest {

  @SuppressWarnings("resource")
  @Test
  void testIsValidUsernamePassword() {

    DslConfig dslConfig = new DslConfig();
    dslConfig.setUrl("url");
    dslConfig.setUsername("user");
    dslConfig.setPassword("password");

    Validator v = Validation.buildDefaultValidatorFactory().getValidator();

    // 2 extra violations because of stateConfig, lockConfig being null
    Set<ConstraintViolation<DslConfig>> violations = v.validate(dslConfig);
    assertEquals(2, violations.size());

    dslConfig.setPassword("");
    violations = v.validate(dslConfig);
    assertEquals(3, violations.size());

    dslConfig.setUsername("");
    violations = v.validate(dslConfig);
    assertEquals(2, violations.size());

    dslConfig.setPassword("password");
    violations = v.validate(dslConfig);
    assertEquals(3, violations.size());
  }
}
