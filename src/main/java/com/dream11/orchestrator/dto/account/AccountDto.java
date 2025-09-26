package com.dream11.orchestrator.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AccountDto {
  @NotNull @Valid Account account = new Account();

  @JsonProperty("linked_accounts")
  @NotNull
  List<@Valid Account> linkedAccounts = new ArrayList<>();
}
