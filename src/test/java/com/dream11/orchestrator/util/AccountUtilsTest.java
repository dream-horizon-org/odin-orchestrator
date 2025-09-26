package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.account.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AccountUtilsTest {

  @ParameterizedTest
  @MethodSource("services")
  void testHasServiceWithCategory(Account account, String category, boolean expected) {
    // Act
    boolean result = AccountUtils.hasServiceWithCategory(account, category);
    // Assert
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> services() {
    Account account = new Account();
    account.setServices(List.of(new Service("TEST", "category", Map.of("old", "old"))));
    return Stream.of(
        Arguments.of(account, "category", true), Arguments.of(account, "NON_EXISTENT", false));
  }
}
