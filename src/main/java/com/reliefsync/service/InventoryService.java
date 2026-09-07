package com.reliefsync.service;

import com.reliefsync.exception.BusinessRuleException;
import com.reliefsync.exception.NotFoundException;
import com.reliefsync.exception.ValidationException;
import com.reliefsync.model.AuditEvent;
import com.reliefsync.model.CenterInventory;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.Resource;
import com.reliefsync.model.enums.Permission;
import com.reliefsync.model.search.InventorySearchCriteria;
import com.reliefsync.repository.CenterInventoryRepository;
import com.reliefsync.repository.InventoryQueryRepository;
import com.reliefsync.repository.InventoryTransactionRepository;
import com.reliefsync.repository.ReliefCenterRepository;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.SearchLimits;
import com.reliefsync.security.AuthorizationService;
import com.reliefsync.security.UserSession;
import com.reliefsync.service.dto.InventoryAdjustmentResult;
import com.reliefsync.service.dto.InventoryOverview;
import java.time.LocalDateTime;
import java.util.List;

public final class InventoryService {
  private final CenterInventoryRepository inventory;
  private final InventoryTransactionRepository transactions;
  private final InventoryQueryRepository queries;
  private final ReliefCenterRepository centers;
  private final ResourceRepository resources;
  private final AuthorizationService authorization;

  public InventoryService(
      CenterInventoryRepository inventory,
      InventoryTransactionRepository transactions,
      InventoryQueryRepository queries,
      ReliefCenterRepository centers,
      ResourceRepository resources,
      AuthorizationService authorization) {
    this.inventory = inventory;
    this.transactions = transactions;
    this.queries = queries;
    this.centers = centers;
    this.resources = resources;
    this.authorization = authorization;
  }

  public CenterInventory initializeStock(
      long centerId, long resourceId, long quantity, String reason, UserSession session) {
    authorization.require(session, Permission.MANAGE_INVENTORY);
    ReliefCenter center = requireCenter(centerId);
    Resource resource = requireResource(resourceId);
    requireReason(reason);
    if (quantity < 0) throw new ValidationException("Initial quantity cannot be negative.");
    if (inventory.findByCenterAndResource(centerId, resourceId).isPresent())
      throw new ValidationException("Inventory for this center and resource already exists.");
    return transactions.execute(
        u -> {
          if (u.find(centerId, resourceId).isPresent())
            throw new ValidationException("Inventory for this center and resource already exists.");
          CenterInventory value =
              new CenterInventory(0, centerId, resourceId, quantity, 0, 0, LocalDateTime.now());
          long id = u.save(value);
          CenterInventory saved =
              new CenterInventory(id, centerId, resourceId, quantity, 0, 0, value.updatedAt());
          u.saveAudit(
              audit(
                  session,
                  id,
                  "Initialized "
                      + resource.name()
                      + " stock at "
                      + center.name()
                      + " to "
                      + quantity
                      + ". Reason: "
                      + reason.trim()
                      + "."));
          return saved;
        });
  }

  public InventoryAdjustmentResult adjustStock(
      long centerId, long resourceId, long delta, String reason, UserSession session) {
    authorization.require(session, Permission.MANAGE_INVENTORY);
    ReliefCenter center = requireCenter(centerId);
    Resource resource = requireResource(resourceId);
    requireReason(reason);
    CenterInventory snapshot =
        inventory
            .findByCenterAndResource(centerId, resourceId)
            .orElseThrow(() -> new NotFoundException("Inventory record was not found."));
    validateNewTotal(snapshot, delta);
    CenterInventory updated =
        transactions.execute(
            u -> {
              CenterInventory current =
                  u.find(centerId, resourceId)
                      .orElseThrow(() -> new NotFoundException("Inventory record was not found."));
              long next = validateNewTotal(current, delta);
              CenterInventory value =
                  new CenterInventory(
                      current.id(),
                      centerId,
                      resourceId,
                      next,
                      current.reservedQuantity(),
                      current.dispatchedQuantity(),
                      LocalDateTime.now());
              u.update(value);
              String sign = delta >= 0 ? "+" : "";
              u.saveAudit(
                  audit(
                      session,
                      value.id(),
                      "Adjusted "
                          + resource.name()
                          + " stock at "
                          + center.name()
                          + " from "
                          + current.totalQuantity()
                          + " to "
                          + next
                          + ". Delta: "
                          + sign
                          + delta
                          + ". Reason: "
                          + reason.trim()
                          + "."));
              return value;
            });
    boolean low = updated.getAvailableQuantity() <= resource.minimumStockThreshold();
    return new InventoryAdjustmentResult(updated, low, resource.minimumStockThreshold());
  }

  public List<InventoryOverview> search(InventorySearchCriteria criteria, UserSession session) {
    authorization.require(session, Permission.VIEW_INVENTORY);
    return queries.search(criteria, SearchLimits.DEFAULT);
  }

  private ReliefCenter requireCenter(long id) {
    return centers
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Relief center was not found: " + id));
  }

  private Resource requireResource(long id) {
    return resources
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Resource was not found: " + id));
  }

  private static long validateNewTotal(CenterInventory i, long delta) {
    long next;
    try {
      next = Math.addExact(i.totalQuantity(), delta);
    } catch (ArithmeticException e) {
      throw new ValidationException("Stock adjustment is too large.");
    }
    if (next < 0) throw new BusinessRuleException("Stock total cannot be negative.");
    if (next < i.reservedQuantity() + i.dispatchedQuantity())
      throw new BusinessRuleException("Stock total cannot be below reserved and dispatched stock.");
    return next;
  }

  private static void requireReason(String reason) {
    if (reason == null || reason.isBlank())
      throw new ValidationException("Adjustment reason is required.");
  }

  private static AuditEvent audit(UserSession session, long id, String description) {
    return new AuditEvent(
        0,
        session.userId(),
        "INVENTORY_STOCK_ADJUSTED",
        "CENTER_INVENTORY",
        id,
        description,
        LocalDateTime.now());
  }
}
