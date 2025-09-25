package com.dream11.orchestrator.dto.account;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service {
  String name;
  String category;
  Map<String, Object> data;
}
