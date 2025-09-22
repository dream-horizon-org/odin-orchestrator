package com.dream11.orchestrator.util;

import static com.dream11.orchestrator.exception.OrchestratorExceptionType.INVALID_ACCOUNT_OBJECT;

import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.account.Service;
import com.dream11.orchestrator.dto.request.ComponentAction;
import com.dream11.orchestrator.exception.OrchestratorException;
import com.dream11.orchestrator.inject.AppContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountUtils {

  public <T> T getServiceWithCategory(List<Service> services, String category, Class<T> clazz) {
    Map<String, Object> data =
        services.stream()
            .filter(service -> service.getCategory().equals(category))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format("No service with category:[%s] found", category)))
            .getData();
    return AppContext.getObjectMapper().convertValue(data, clazz);
  }

  public boolean hasServiceWithCategory(Account account, String category) {
    return account.getServices().stream()
        .anyMatch(service -> service.getCategory().equals(category));
  }

  public void validateAccount(List<ComponentAction> componentActions) {

    componentActions.forEach(
        componentAction -> {
          if (componentAction.getAccounts() == null
              || componentAction.getAccounts().getAccount() == null
              || componentAction.getAccounts().getAccount().getProvider() == null
              || componentAction.getProvider() == null
              || !componentAction
                  .getProvider()
                  .equalsIgnoreCase(componentAction.getAccounts().getAccount().getProvider())) {
            throw new OrchestratorException(INVALID_ACCOUNT_OBJECT);
          }
        });
  }

  public Map<String, String> getResourceLabels(Account account) {
    return (Map<String, String>)
        account.getAccountData().getOrDefault("resourceLabels", new HashMap<>());
  }

  public Map<String, String> getRunnerServiceAccountAnnotations(Account account) {
    return (Map<String, String>)
        account.getAccountData().getOrDefault("runnerServiceAccountAnnotations", new HashMap<>());
  }
}
