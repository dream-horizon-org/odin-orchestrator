package com.dream11.orchestrator.dto.request;

import com.dream11.orchestrator.dto.account.Account;
import com.dream11.orchestrator.dto.constants.NamespaceAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NamespaceRequestMessageBody implements RequestMessageBody {
  Account account;
  NamespaceAction action;
  String name;
  long orgId;
}
