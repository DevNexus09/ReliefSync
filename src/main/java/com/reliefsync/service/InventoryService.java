package com.reliefsync.service;

import com.reliefsync.model.StockView;
import com.reliefsync.model.User;
import com.reliefsync.repository.InventoryRepository;
import java.util.List;

public class InventoryService {

    private final InventoryRepository inventory = new InventoryRepository();

    public List<StockView> stockForCenter(long centerId) {
        return inventory.stockForCenter(centerId);
    }

    public List<StockView> lowStock() {
        return inventory.lowStock();
    }

    public void setQuantity(User actor, long centerId, long resourceId, int quantity) {
        AccessControl.require(actor, Feature.INVENTORY);
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        inventory.upsertQuantity(centerId, resourceId, quantity);
    }

    public void adjust(User actor, long centerId, long resourceId, int delta) {
        AccessControl.require(actor, Feature.INVENTORY);
        if (delta == 0) {
            throw new IllegalArgumentException("Adjustment must not be zero");
        }
        inventory.adjust(centerId, resourceId, delta);
    }
}
