package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.account.Service;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.exception.OrchestratorExceptionType;
import com.dream11.orchestrator.inject.AppContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AccountUtilsTest {

  final ObjectMapper objectMapper = AppContext.getObjectMapper();

  @ParameterizedTest
  @MethodSource("invalidAccountData")
  void testAccountValidationFailIfInvalidAccountData(String accountData)
      throws JsonProcessingException {
    // Arrange
    ComponentAction componentAction =
        this.objectMapper.readValue(
            TestUtil.getComponentAction("comp1", "validate", accountData).toString(),
            ComponentAction.class);

    // Act & Assert
    assertThatThrownBy(() -> AccountUtils.validateAccount(List.of(componentAction)))
        .isInstanceOf(OrchestratorException.class)
        .hasMessage(OrchestratorExceptionType.INVALID_ACCOUNT_OBJECT.getErrorMessage());
  }

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

  private static Stream<Arguments> invalidAccountData() {
    return Stream.of(
        Arguments.of("{}"),
        Arguments.of(
            """
                {
                  "account": {
                    "provider": "sample"
                  }
                }
                """),
        Arguments.of(
            """
                {
                  "account": {}
                }
                """));
  }
}
