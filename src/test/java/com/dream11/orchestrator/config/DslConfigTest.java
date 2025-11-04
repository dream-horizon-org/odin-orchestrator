package com.dream11.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.config.dsl.DslConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DslConfigTest {

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
    assertThat(2).isEqualTo(violations.size());

    dslConfig.setPassword("");
    violations = v.validate(dslConfig);
    assertThat(3).isEqualTo(violations.size());

    dslConfig.setUsername("");
    violations = v.validate(dslConfig);
    assertThat(2).isEqualTo(violations.size());

    dslConfig.setPassword("password");
    violations = v.validate(dslConfig);
    assertThat(3).isEqualTo(violations.size());
  }
}
