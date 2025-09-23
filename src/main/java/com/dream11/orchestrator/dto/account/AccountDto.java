package com.dream11.orchestrator.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AccountDto {
  Account account = new Account();

  @JsonProperty("linked_accounts")
  List<Account> linkedAccounts = new ArrayList<>();
}
