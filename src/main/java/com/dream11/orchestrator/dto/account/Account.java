package com.dream11.orchestrator.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {
  @With
  @JsonProperty("data")
  Map<String, Object> accountData;

  String category;

  @JsonProperty("default")
  Boolean isDefault;

  String name;
  String provider;
  List<Service> services = new ArrayList<>();
}
