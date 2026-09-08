package com.reliefsync.model.request;

import com.reliefsync.model.enums.RequestPriority;
import java.util.ArrayList;
import java.util.List;

public final class ReliefRequestDraft {
  private long disasterEventId;
  private long affectedAreaId;
  private RequestPriority priority;
  private String description;
  private final List<ReliefRequestDraftItem> items;

  public ReliefRequestDraft(
      long disasterEventId,
      long affectedAreaId,
      RequestPriority priority,
      String description,
      List<ReliefRequestDraftItem> items) {
    this.disasterEventId = disasterEventId;
    this.affectedAreaId = affectedAreaId;
    this.priority = priority;
    this.description = description;
    this.items = new ArrayList<>(items == null ? List.of() : items);
  }

  public long disasterEventId() {
    return disasterEventId;
  }

  public void setDisasterEventId(long value) {
    disasterEventId = value;
  }

  public long affectedAreaId() {
    return affectedAreaId;
  }

  public void setAffectedAreaId(long value) {
    affectedAreaId = value;
  }

  public RequestPriority priority() {
    return priority;
  }

  public void setPriority(RequestPriority value) {
    priority = value;
  }

  public String description() {
    return description;
  }

  public void setDescription(String value) {
    description = value;
  }

  public List<ReliefRequestDraftItem> items() {
    return List.copyOf(items);
  }

  public void addItem(ReliefRequestDraftItem item) {
    items.add(item);
  }

  public void removeItem(int index) {
    items.remove(index);
  }
}
