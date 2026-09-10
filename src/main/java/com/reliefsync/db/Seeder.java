package com.reliefsync.db;

import com.reliefsync.model.AffectedArea;
import com.reliefsync.model.DeliveryRecoveryAction;
import com.reliefsync.model.DraftItem;
import com.reliefsync.model.Priority;
import com.reliefsync.model.ReliefCenter;
import com.reliefsync.model.Resource;
import com.reliefsync.model.Role;
import com.reliefsync.model.User;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.AreaRepository;
import com.reliefsync.repository.CenterRepository;
import com.reliefsync.repository.InventoryRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.ResourceRepository;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.repository.VehicleRepository;
import com.reliefsync.security.PasswordHasher;
import com.reliefsync.service.AllocationService;
import com.reliefsync.service.DispatchService;
import com.reliefsync.service.InventoryService;
import com.reliefsync.service.RequestService;
import java.util.ArrayList;
import java.util.List;

/** Optional, deterministic Bangladesh-context demonstration data. */
public final class Seeder {
    public static final String DEMO_PASSWORD = "ReliefSync@2026";

    private Seeder() { }

    public static boolean seedRequested() {
        return "true".equalsIgnoreCase(System.getenv("RELIEFSYNC_SEED_DEMO"))
                || Boolean.getBoolean("reliefsync.seed");
    }

    public static void seedDemo() {
        seedUsers();
        seedAreas();
        seedCenters();
        seedResources();
        SeedContext data = new SeedContext();
        seedInventory(data);
        seedVehicles();
        seedWorkflowScenarios(data);
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

    private static void seedAreas() {
        seedArea("Sunamganj Sadar Flood Zone", "Sunamganj", 42_000, 5);
        seedArea("Tahirpur Haor Belt", "Sunamganj", 28_500, 5);
        seedArea("Companiganj Flood Shelters", "Sylhet", 33_700, 4);
        seedArea("Chilmari Char Union", "Kurigram", 26_400, 5);
        seedArea("Fulchhari Riverbank", "Gaibandha", 21_800, 4);
        seedArea("Shyamnagar Coastal Belt", "Satkhira", 38_200, 5);
        seedArea("Koyra South", "Khulna", 29_400, 4);
        seedArea("Mongla Riverside", "Bagerhat", 19_600, 3);
        seedArea("Halishahar Waterlogged Zone", "Chattogram", 44_800, 4);
    }

    private static void seedArea(String name, String district, int population, int severity) {
        AreaRepository areas = new AreaRepository();
        if (areas.search(name).stream().noneMatch(area -> area.name().equals(name))) {
            areas.insert(name, district, population, severity);
        }
    }

    private static void seedCenters() {
        seedCenter("Dhaka Central Relief Warehouse", "Tejgaon, Dhaka", 100_000);
        seedCenter("Sylhet Regional Relief Hub", "Sylhet Sadar", 40_000);
        seedCenter("Bogura Northern Relief Depot", "Bogura Sadar", 45_000);
        seedCenter("Khulna Coastal Relief Depot", "Khalishpur, Khulna", 35_000);
    }

    private static void seedCenter(String name, String location, int capacity) {
        CenterRepository centers = new CenterRepository();
        if (centers.search(name).stream().noneMatch(center -> center.name().equals(name))) {
            centers.insert(name, location, capacity);
        }
    }

    private static void seedResources() {
        seedResource("Safe Drinking Water", "litre", 400);
        seedResource("Rice", "kg", 250);
        seedResource("Dry Food Family Pack", "pack", 100);
        seedResource("Oral Saline", "sachet", 120);
        seedResource("Essential Medicine Kit", "kit", 20);
        seedResource("Hygiene Kit", "kit", 40);
        seedResource("Tarpaulin Sheet", "sheet", 30);
        seedResource("Blanket", "piece", 50);
    }

    private static void seedResource(String name, String unit, int threshold) {
        ResourceRepository resources = new ResourceRepository();
        if (resources.search(name).stream().noneMatch(resource -> resource.name().equals(name))) {
            resources.insert(name, unit, threshold);
        }
    }

    private static void seedInventory(SeedContext data) {
        int[][] quantities = {
                {4_200, 2_400, 900, 900, 90, 220, 260, 400},
                {1_300, 700, 320, 300, 28, 35, 110, 150},
                {1_500, 1_100, 360, 380, 45, 100, 90, 220},
                {950, 650, 240, 180, 16, 70, 25, 45}
        };
        String[] centerNames = {"Dhaka Central Relief Warehouse", "Sylhet Regional Relief Hub",
                "Bogura Northern Relief Depot", "Khulna Coastal Relief Depot"};
        String[] resourceNames = {"Safe Drinking Water", "Rice", "Dry Food Family Pack", "Oral Saline",
                "Essential Medicine Kit", "Hygiene Kit", "Tarpaulin Sheet", "Blanket"};
        InventoryRepository repository = new InventoryRepository();
        for (int center = 0; center < centerNames.length; center++) {
            for (int resource = 0; resource < resourceNames.length; resource++) {
                long centerId = data.center(centerNames[center]).id();
                long resourceId = data.resource(resourceNames[resource]).id();
                if (repository.find(centerId, resourceId).isEmpty()) {
                    data.inventory.setQuantity(data.centerManager, centerId, resourceId,
                            quantities[center][resource]);
                }
            }
        }
    }

    private static void seedVehicles() {
        seedVehicle("DHAKA-METRO-TA-11-2401", "Heavy Cargo Truck", 5_000, VehicleStatus.AVAILABLE);
        seedVehicle("SYLHET-TA-12-3102", "Cargo Truck", 2_600, VehicleStatus.AVAILABLE);
        seedVehicle("KHULNA-TA-11-1804", "Cargo Truck", 2_200, VehicleStatus.AVAILABLE);
        seedVehicle("BOGURA-TA-11-2203", "Medium Truck", 1_800, VehicleStatus.AVAILABLE);
        seedVehicle("DHAKA-METRO-NA-15-4506", "Relief Van", 1_200, VehicleStatus.AVAILABLE);
        seedVehicle("CHATTO-METRO-NA-13-1705", "Relief Van", 900, VehicleStatus.AVAILABLE);
        seedVehicle("SYLHET-NA-11-0907", "Pickup", 600, VehicleStatus.MAINTENANCE);
        seedVehicle("KHULNA-NA-12-0808", "Coastal Van", 700, VehicleStatus.INACTIVE);
    }

    private static void seedVehicle(String registration, String type, int capacity, VehicleStatus status) {
        VehicleRepository vehicles = new VehicleRepository();
        if (vehicles.findByRegistration(registration).isEmpty()) {
            long id = vehicles.insert(registration, type, capacity);
            if (status != VehicleStatus.AVAILABLE) vehicles.setStatus(id, status);
        }
    }

    private static void seedWorkflowScenarios(SeedContext data) {
        data.scenario("[DEMO-R01]", "Halishahar Waterlogged Zone", Priority.NORMAL,
                "Draft: ward shelter assessment awaiting final family count",
                data.items("Hygiene Kit", 80, "Oral Saline", 200), id -> { });

        data.scenario("[DEMO-R02]", "Fulchhari Riverbank", Priority.NORMAL,
                "Submitted Normal: Jamuna riverbank shelters need food and water",
                data.items("Rice", 350, "Safe Drinking Water", 600),
                id -> data.requests.submit(data.volunteer, id));

        // HIGH request intentionally left after round one.
        data.scenario("[DEMO-R03]", "Chilmari Char Union", Priority.HIGH,
                "Submitted High: isolated char households need food supplies",
                data.items("Rice", 700, "Dry Food Family Pack", 220), id -> {
                    data.requests.submit(data.volunteer, id);
                    data.requests.decideVerification(data.areaCoordinator, id, true, "Field headcount confirmed");
                });

        // CRITICAL request intentionally left after rounds one and two.
        data.scenario("[DEMO-R04]", "Shyamnagar Coastal Belt", Priority.CRITICAL,
                "Submitted Critical: cyclone surge damaged shelters and water sources",
                data.items("Safe Drinking Water", 1_000, "Tarpaulin Sheet", 120,
                        "Essential Medicine Kit", 30), id -> {
                    data.requests.submit(data.volunteer, id);
                    data.requests.decideVerification(data.areaCoordinator, id, true,
                            "Cyclone impact verified on site");
                    data.requests.decideVerification(data.reliefCoordinator, id, true,
                            "Regional response capacity confirmed");
                });

        data.scenario("[DEMO-R05]", "Mongla Riverside", Priority.NORMAL,
                "Verified: riverside families awaiting shelter allocation",
                data.items("Tarpaulin Sheet", 100, "Hygiene Kit", 100), data::verifyNormal);

        // Remains VERIFIED so both strategies can be previewed live.
        data.scenario("[DEMO-R06]", "Companiganj Flood Shelters", Priority.HIGH,
                "Strategy comparison: concentrated warehouse supply versus balanced centers",
                data.items("Safe Drinking Water", 900, "Dry Food Family Pack", 300), data::verifyHigh);

        data.scenario("[DEMO-R07]", "Tahirpur Haor Belt", Priority.CRITICAL,
                "Allocated: complete food and blanket reservation for haor shelters",
                data.items("Rice", 500, "Blanket", 120), id -> {
                    data.verifyCritical(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                });

        data.scenario("[DEMO-R08]", "Koyra South", Priority.NORMAL,
                "Dispatched: coastal shelter supplies currently in transit",
                data.items("Dry Food Family Pack", 180, "Hygiene Kit", 80), id -> {
                    data.verifyNormal(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    data.dispatch.dispatch(data.transport, id, data.vehicle("SYLHET-TA-12-3102"), "Faruk Ahmed");
                });

        data.scenario("[DEMO-R09]", "Sunamganj Sadar Flood Zone", Priority.CRITICAL,
                "Delivered: emergency water, saline and medicine reached flood shelters",
                data.items("Safe Drinking Water", 800, "Oral Saline", 300,
                        "Essential Medicine Kit", 25), id -> {
                    data.verifyCritical(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    data.dispatch.dispatch(data.transport, id,
                            data.vehicle("DHAKA-METRO-TA-11-2401"), "Kamal Hossain");
                    data.dispatch.deliver(data.transport, id);
                });

        data.scenario("[DEMO-R10]", "Chilmari Char Union", Priority.NORMAL,
                "Rejected: duplicate shelter list already covered by local distribution",
                data.items("Rice", 200, "Safe Drinking Water", 300), id -> {
                    data.requests.submit(data.volunteer, id);
                    data.requests.decideVerification(data.areaCoordinator, id, false,
                            "Shelter list duplicates an existing verified assessment");
                });

        data.scenario("[DEMO-R11]", "Halishahar Waterlogged Zone", Priority.NORMAL,
                "Cancelled after allocation: access restored before dispatch",
                data.items("Tarpaulin Sheet", 90, "Blanket", 100), id -> {
                    data.verifyNormal(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    data.requests.cancel(data.volunteer, id);
                });

        data.scenario("[DEMO-R12]", "Shyamnagar Coastal Belt", Priority.HIGH,
                "Delivery failed: cargo recovered pending return for reallocation",
                data.items("Safe Drinking Water", 700, "Hygiene Kit", 100), id -> {
                    data.verifyHigh(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    data.dispatch.dispatch(data.transport, id,
                            data.vehicle("KHULNA-TA-11-1804"), "Shahidul Islam");
                    data.dispatch.reportDeliveryFailure(data.transport, id,
                            "Coastal access road became impassable after tidal flooding",
                            DeliveryRecoveryAction.REALLOCATE,
                            "Cargo returned to a temporary holding point for coordinator review");
                });

        data.scenario("[DEMO-R13]", "Fulchhari Riverbank", Priority.NORMAL,
                "Retry active: preserved failed attempt followed by replacement vehicle",
                data.items("Rice", 350, "Dry Food Family Pack", 120), id -> {
                    data.verifyNormal(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    data.dispatch.dispatch(data.transport, id,
                            data.vehicle("BOGURA-TA-11-2203"), "Imran Kabir");
                    data.dispatch.reportDeliveryFailure(data.transport, id,
                            "Vehicle developed a cooling-system fault near the river crossing",
                            DeliveryRecoveryAction.RETRY, "Cargo remained intact for transfer");
                    data.dispatch.retryDelivery(data.transport, id,
                            data.vehicle("DHAKA-METRO-NA-15-4506"), "Nasir Uddin");
                });

        // Initial partial allocation, stock receipt, and separate reallocation audit event.
        data.scenario("[DEMO-R15]", "Mongla Riverside", Priority.NORMAL,
                "Reallocation: newly received medicine reduces outstanding need",
                data.items("Essential Medicine Kit", 220, "Dry Food Family Pack", 100), id -> {
                    data.verifyNormal(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                    ReliefCenter bogura = data.center("Bogura Northern Relief Depot");
                    Resource medicine = data.resource("Essential Medicine Kit");
                    data.inventory.adjust(data.centerManager, bogura.id(), medicine.id(), 30);
                    data.allocations.reallocate(data.reliefCoordinator, id, "Fewest Centers");
                });

        // Seeded after R15 so medicine is unavailable and remains outstanding.
        data.scenario("[DEMO-R14]", "Tahirpur Haor Belt", Priority.HIGH,
                "Partial allocation: medicine shortage remains after food reservation",
                data.items("Essential Medicine Kit", 100, "Safe Drinking Water", 200, "Rice", 200), id -> {
                    data.verifyHigh(id);
                    data.allocations.allocate(data.reliefCoordinator, id, "Fewest Centers");
                });

        // Same area as open VERIFIED R06; the UI's duplicate check returns true.
        data.scenario("[DEMO-R16]", "Companiganj Flood Shelters", Priority.NORMAL,
                "Probable duplicate: second open assessment for the same flood shelters",
                data.items("Safe Drinking Water", 250, "Dry Food Family Pack", 80),
                id -> data.requests.submit(data.volunteer, id));
    }

    @FunctionalInterface
    private interface ScenarioAction { void run(long requestId); }

    private static final class SeedContext {
        private final UserRepository users = new UserRepository();
        private final AreaRepository areas = new AreaRepository();
        private final CenterRepository centers = new CenterRepository();
        private final ResourceRepository resources = new ResourceRepository();
        private final VehicleRepository vehicles = new VehicleRepository();
        private final RequestRepository requestRepository = new RequestRepository();
        private final RequestService requests = new RequestService();
        private final AllocationService allocations = new AllocationService();
        private final DispatchService dispatch = new DispatchService();
        private final InventoryService inventory = new InventoryService();
        private final User admin = user("admin");
        private final User areaCoordinator = user("area_coordinator");
        private final User centerManager = user("center_manager");
        private final User transport = user("transport");
        private final User volunteer = user("volunteer");
        private final User reliefCoordinator = user("relief_coordinator");

        private void scenario(String key, String areaName, Priority priority, String description,
                              List<DraftItem> items, ScenarioAction action) {
            if (requestRepository.findByNotePrefix(key).isPresent()) return;
            long id = requests.createDraft(volunteer, area(areaName).id(), priority,
                    key + " " + description, items);
            action.run(id);
        }

        private void verifyNormal(long requestId) {
            requests.submit(volunteer, requestId);
            requests.decideVerification(areaCoordinator, requestId, true, "Local assessment verified");
        }

        private void verifyHigh(long requestId) {
            verifyNormal(requestId);
            requests.decideVerification(reliefCoordinator, requestId, true, "Operational capacity confirmed");
        }

        private void verifyCritical(long requestId) {
            verifyHigh(requestId);
            requests.decideVerification(admin, requestId, true, "Approved for emergency priority release");
        }

        private List<DraftItem> items(Object... values) {
            List<DraftItem> result = new ArrayList<>();
            for (int index = 0; index < values.length; index += 2) {
                Resource resource = resource((String) values[index]);
                result.add(new DraftItem(resource.id(), resource.name(), (Integer) values[index + 1]));
            }
            return List.copyOf(result);
        }

        private User user(String username) {
            return users.findByUsername(username).orElseThrow();
        }

        private AffectedArea area(String name) {
            return areas.search(name).stream().filter(area -> area.name().equals(name)).findFirst().orElseThrow();
        }

        private ReliefCenter center(String name) {
            return centers.search(name).stream().filter(center -> center.name().equals(name))
                    .findFirst().orElseThrow();
        }

        private Resource resource(String name) {
            return resources.search(name).stream().filter(resource -> resource.name().equals(name))
                    .findFirst().orElseThrow();
        }

        private long vehicle(String registration) {
            return vehicles.findByRegistration(registration).orElseThrow().id();
        }
    }
}
