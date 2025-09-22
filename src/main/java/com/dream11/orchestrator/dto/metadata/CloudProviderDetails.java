package com.dream11.orchestrator.dto.metadata;

import com.dream11.orchestrator.dto.account.Account;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudProviderDetails {
  Account account;

  @JsonProperty("linked_accounts")
  List<Account> linkedAccounts;
}
