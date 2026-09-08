package com.reliefsync.model;

/** One inventory line joined with its center and resource, for display and planning. */
public record StockView(long centerId, String centerName, long resourceId, String resourceName,
                        int quantity, int lowStockThreshold) {

    public String statusLabel() {
        if (quantity == 0) {
            return "OUT OF STOCK";
        }
        return quantity <= lowStockThreshold ? "LOW" : "OK";
    }
}
