package com.dream11.orchestrator.dto.request;

import com.dream11.orchestrator.constants.NamespaceAction;
import com.dream11.orchestrator.dto.account.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NamespaceRequestMessageBody implements RequestMessageBody {
  @NotNull @Valid Account account;
  @NotNull NamespaceAction action;
  @NotBlank String name;
  long orgId;
}
