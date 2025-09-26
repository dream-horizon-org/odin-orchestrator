package com.dream11.orchestrator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.account.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AccountUtilTest {

  @ParameterizedTest
  @MethodSource("services")
  void testHasServiceWithCategory(Account account, String category, boolean expected) {
    // Act
    boolean result = AccountUtil.hasServiceWithCategory(account, category);
    // Assert
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("resourceLabels")
  void testGetResourceLabels(Account account, Map<String, String> expectedLabels) {
    // Act
    Map<String, String> resourceLabels = AccountUtil.getResourceLabels(account);
    // Assert
    assertThat(resourceLabels).containsExactlyInAnyOrderEntriesOf(expectedLabels);
  }

  @ParameterizedTest
  @MethodSource("runnerServiceAccountAnnotations")
  void testGetRunnerServiceAccountAnnotations(
      Account account, Map<String, String> expectedAnnotations) {
    // Act
    Map<String, String> runnerServiceAccountAnnotations =
        AccountUtil.getRunnerServiceAccountAnnotations(account);
    // Assert
    assertThat(runnerServiceAccountAnnotations)
        .containsExactlyInAnyOrderEntriesOf(expectedAnnotations);
  }

  private static Stream<Arguments> services() {
    Account account =
        Account.builder()
            .services(List.of(new Service("TEST", "category", Map.of("old", "old"))))
            .build();
    return Stream.of(
        Arguments.of(account, "category", true), Arguments.of(account, "NON_EXISTENT", false));
  }

  private static Stream<Arguments> resourceLabels() {
    return Stream.of(
        Arguments.of(
            Account.builder().accountData(Map.of("resourceLabels", Map.of("k", "v"))).build(),
            Map.of("k", "v")),
        Arguments.of(Account.builder().build(), Map.of()));
  }

  private static Stream<Arguments> runnerServiceAccountAnnotations() {
    return Stream.of(
        Arguments.of(
            Account.builder()
                .accountData(Map.of("runnerServiceAccountAnnotations", Map.of("k", "v")))
                .build(),
            Map.of("k", "v")),
        Arguments.of(Account.builder().build(), Map.of()));
  }

  static class SampleService {
    public String key;
  }

  @Test
  void testGetServiceWithCategory() {
    // Arrange
    List<Service> services = List.of(new Service("name", "category", Map.of("key", "value")));

    // Act
    SampleService data =
        AccountUtil.getServiceWithCategory(services, "category", SampleService.class);

    // Assert
    assertThat(data.key).isEqualTo("value");
  }

  @Test
  void testGetServiceWithCategoryMissingCategory() {
    // Arrange
    List<Service> services = List.of(new Service("name", "category", Map.of("key", "value")));

    // Act & Assert
    assertThatThrownBy(
            () -> AccountUtil.getServiceWithCategory(services, "missing", SampleService.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No service with category:[missing] found");
  }
}
