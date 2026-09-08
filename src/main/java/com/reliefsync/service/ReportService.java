package com.reliefsync.service;

import com.reliefsync.model.AreaFulfillment;
import com.reliefsync.model.RequestRow;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.StatusCount;
import com.reliefsync.model.StockView;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import java.util.List;

/** Read-only search and analytics used by the reports and dashboard screens. */
public class ReportService {

    private final RequestRepository requests = new RequestRepository();
    private final InventoryRepository inventory = new InventoryRepository();

    public List<StockView> lowStock() {
        return inventory.lowStock();
    }

    public List<StatusCount> statusSummary() {
        return requests.statusSummary();
    }

    public List<AreaFulfillment> areaFulfillment() {
        return requests.areaFulfillment();
    }

    public List<RequestRow> searchRequests(String text, RequestStatus statusOrNull) {
        return requests.rows(text == null ? "" : text, statusOrNull);
    }

    public int countWithStatus(RequestStatus... statuses) {
        List<StatusCount> summary = statusSummary();
        int total = 0;
        for (RequestStatus wanted : statuses) {
            total += summary.stream()
                    .filter(s -> s.status() == wanted)
                    .mapToInt(StatusCount::count)
                    .sum();
        }
        return total;
    }
}
