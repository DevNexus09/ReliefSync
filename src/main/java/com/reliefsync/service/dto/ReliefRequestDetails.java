package com.reliefsync.service.dto;

import com.reliefsync.model.ReliefRequest;
import com.reliefsync.model.ReliefRequestItem;
import java.util.List;

public record ReliefRequestDetails(ReliefRequest request, List<ReliefRequestItem> items) {
  public ReliefRequestDetails {
    items = List.copyOf(items);
  }
}
