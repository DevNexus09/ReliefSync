package com.reliefsync.db;

import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.CenterRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.service.AllocationService;
import com.reliefsync.service.RequestService;
import java.util.List;

/**
 * Optional, idempotent development seeding. Runs only when requested
 * (RELIEFSYNC_SEED_DEMO=true or -Dreliefsync.seed=true) and inserts each demo
 * record only if it does not already exist, so repeated runs are safe.
 */
public final class Seeder {

    public static final String DEMO_PASSWORD = "ReliefSync@2026";

    private Seeder() {
    }

    public static boolean seedRequested() {
        return "true".equalsIgnoreCase(System.getenv("RELIEFSYNC_SEED_DEMO"))
                || Boolean.getBoolean("reliefsync.seed");
    }

    public static void seedDemo() {
        seedUsers();
        seedMasterData();
        seedRequests();
    }

    private static void seedUsers() {
        UserRepository users = new UserRepository();
        seedUser(users, "admin", "System Administrator", Role.ADMIN);
        seedUser(users, "area_coordinator", "Ayesha Rahman", Role.AREA_COORDINATOR);
        seedUser(users, "center_manager", "Mahmud Hasan", Role.CENTER_MANAGER);
        seedUser(users, "transport", "Rafiq Islam", Role.TRANSPORT_COORDINATOR);
        seedUser(users, "volunteer", "Nusrat Jahan", Role.VOLUNTEER);
        seedUser(users, "relief_coordinator", "Tanvir Ahmed", Role.RELIEF_COORDINATOR);
    }

    private static void seedUser(UserRepository users, String username, String fullName, Role role) {
        if (users.findByUsername(username).isEmpty()) {
            users.insert(username, fullName, role, PasswordHasher.hash(DEMO_PASSWORD));
        }
    }

    private static void seedMasterData() {
        AreaRepository areas = new AreaRepository();
        if (areas.search("").isEmpty()) {
            areas.insert("Sunamganj Sadar", "Sunamganj", 42000, 5);
            areas.insert("Kurigram North", "Kurigram", 31000, 4);
            areas.insert("Khulna Coastal Belt", "Khulna", 56000, 3);
        }

        CenterRepository centers = new CenterRepository();
        if (centers.search("").isEmpty()) {
            centers.insert("Central Warehouse Dhaka", "Tejgaon, Dhaka", 100000);
            centers.insert("Sylhet Relief Hub", "Sylhet Sadar", 40000);
            centers.insert("Khulna Depot", "Khalishpur, Khulna", 30000);
        }

        ResourceRepository resources = new ResourceRepository();
        if (resources.search("").isEmpty()) {
            resources.insert("Drinking Water", "litre", 500);
            resources.insert("Rice", "kg", 300);
            resources.insert("Dry Food Pack", "pack", 100);
            resources.insert("Medicine Kit", "kit", 20);
            resources.insert("Tarpaulin", "sheet", 30);
            resources.insert("Blanket", "piece", 50);
        }

        InventoryRepository inventory = new InventoryRepository();
        if (inventory.availableStock().isEmpty()) {
            // center ids 1..3 and resource ids 1..6 as inserted above
            int[][] stock = {
                    // water, rice, food, medicine, tarpaulin, blanket
                    {5000, 2000, 800, 60, 150, 300},   // Central Warehouse Dhaka
                    {1500, 700, 250, 25, 60, 120},     // Sylhet Relief Hub
                    {900, 400, 120, 10, 20, 40},       // Khulna Depot
            };
            for (int centerIdx = 0; centerIdx < stock.length; centerIdx++) {
                for (int resIdx = 0; resIdx < stock[centerIdx].length; resIdx++) {
                    inventory.upsertQuantity(centerIdx + 1, resIdx + 1, stock[centerIdx][resIdx]);
                }
            }
        }
    }

    /**
     * Drives the real workflow services to leave demo requests resting in
     * different lifecycle states, so a fresh database already demonstrates the
     * State, Chain of Responsibility, and Strategy patterns end to end.
     */
    private static void seedRequests() {
        RequestRepository requests = new RequestRepository();
        if (!requests.rows("", null).isEmpty()) {
            return;
        }

        UserRepository users = new UserRepository();
        User volunteer = users.findByUsername("volunteer").orElseThrow();
        User areaCoordinator = users.findByUsername("area_coordinator").orElseThrow();
        User reliefCoordinator = users.findByUsername("relief_coordinator").orElseThrow();
        User admin = users.findByUsername("admin").orElseThrow();
        User transport = users.findByUsername("transport").orElseThrow();

        RequestService requestService = new RequestService();
        AllocationService allocationService = new AllocationService();

        // 1. CRITICAL, three approval rounds, carried all the way to DELIVERED.
        long delivered = requestService.createDraft(volunteer, 1, Priority.CRITICAL,
                "Flash flood — families stranded without safe water",
                List.of(new DraftItem(1, "Drinking Water", 1200),
                        new DraftItem(4, "Medicine Kit", 25)));
        requestService.submit(volunteer, delivered);
        requestService.decideVerification(areaCoordinator, delivered, true, "Verified on site");
        requestService.decideVerification(reliefCoordinator, delivered, true, "Stock available at Dhaka");
        requestService.decideVerification(admin, delivered, true, "Approved for immediate release");
        allocationService.allocate(reliefCoordinator, delivered, "Fewest Centers");
        allocationService.dispatch(transport, delivered);
        allocationService.deliver(transport, delivered);

        // 2. NORMAL, single round approved, waiting for allocation.
        long verified = requestService.createDraft(volunteer, 3, Priority.NORMAL,
                "Shelter materials for displaced families",
                List.of(new DraftItem(5, "Tarpaulin", 60),
                        new DraftItem(6, "Blanket", 90)));
        requestService.submit(volunteer, verified);
        requestService.decideVerification(areaCoordinator, verified, true, "Assessment complete");

        // 3. HIGH, first round approved, still awaiting the relief coordinator.
        long awaitingRound2 = requestService.createDraft(volunteer, 2, Priority.HIGH,
                "Food supplies for embankment shelters",
                List.of(new DraftItem(2, "Rice", 500),
                        new DraftItem(3, "Dry Food Pack", 150)));
        requestService.submit(volunteer, awaitingRound2);
        requestService.decideVerification(areaCoordinator, awaitingRound2, true, "Headcount confirmed");

        // 4. Rejected after the first round, for the audit trail.
        long rejected = requestService.createDraft(volunteer, 2, Priority.NORMAL,
                "Duplicate submission for the same shelter",
                List.of(new DraftItem(2, "Rice", 100)));
        requestService.submit(volunteer, rejected);
        requestService.decideVerification(areaCoordinator, rejected, false,
                "Already covered by an earlier request");

        // 5. An untouched draft.
        requestService.createDraft(volunteer, 1, Priority.HIGH,
                "Medicine top-up — awaiting final headcount",
                List.of(new DraftItem(4, "Medicine Kit", 15)));
    }
}
