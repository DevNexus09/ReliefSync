PRAGMA foreign_keys = ON;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN (
        'ADMINISTRATOR', 'RELIEF_COORDINATOR', 'AREA_COORDINATOR',
        'VOLUNTEER', 'RELIEF_CENTER_MANAGER', 'TRANSPORT_COORDINATOR'
    )),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL
);

CREATE TABLE disaster_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('FLOOD', 'CYCLONE', 'WATERLOGGING', 'EMERGENCY')),
    description TEXT,
    start_date TEXT NOT NULL,
    end_date TEXT,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED')),
    created_by INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    CHECK (end_date IS NULL OR end_date >= start_date),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE affected_areas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    disaster_event_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    district TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    population_affected INTEGER NOT NULL DEFAULT 0 CHECK (population_affected >= 0),
    families_affected INTEGER NOT NULL DEFAULT 0 CHECK (families_affected >= 0),
    severity TEXT NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    accessibility TEXT NOT NULL CHECK (accessibility IN ('ACCESSIBLE', 'PARTIALLY_ACCESSIBLE', 'INACCESSIBLE')),
    medical_urgency TEXT NOT NULL CHECK (medical_urgency IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    water_access TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    FOREIGN KEY (disaster_event_id) REFERENCES disaster_events(id) ON DELETE RESTRICT
);

CREATE TABLE relief_centers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    district TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    contact_info TEXT,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    unit TEXT NOT NULL,
    minimum_stock_threshold INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock_threshold >= 0),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    UNIQUE (name, unit)
);

CREATE TABLE center_inventory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    relief_center_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    total_quantity INTEGER NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    dispatched_quantity INTEGER NOT NULL DEFAULT 0 CHECK (dispatched_quantity >= 0),
    updated_at TEXT NOT NULL,
    CHECK (reserved_quantity + dispatched_quantity <= total_quantity),
    FOREIGN KEY (relief_center_id) REFERENCES relief_centers(id) ON DELETE RESTRICT,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE RESTRICT
);

CREATE TABLE relief_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    disaster_event_id INTEGER NOT NULL,
    affected_area_id INTEGER NOT NULL,
    requested_by INTEGER NOT NULL,
    priority TEXT NOT NULL CHECK (priority IN ('NORMAL', 'HIGH', 'CRITICAL')),
    state TEXT NOT NULL CHECK (state IN (
        'SUBMITTED', 'UNDER_VERIFICATION', 'VERIFIED', 'PARTIALLY_ALLOCATED',
        'ALLOCATED', 'DISPATCHED', 'DELIVERED', 'RETURNED', 'REJECTED', 'CANCELLED'
    )),
    description TEXT,
    submitted_at TEXT,
    verified_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (disaster_event_id) REFERENCES disaster_events(id) ON DELETE RESTRICT,
    FOREIGN KEY (affected_area_id) REFERENCES affected_areas(id) ON DELETE RESTRICT,
    FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE relief_request_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    requested_quantity INTEGER NOT NULL CHECK (requested_quantity > 0),
    allocated_quantity INTEGER NOT NULL DEFAULT 0 CHECK (allocated_quantity >= 0),
    delivered_quantity INTEGER NOT NULL DEFAULT 0 CHECK (delivered_quantity >= 0),
    CHECK (allocated_quantity <= requested_quantity),
    CHECK (delivered_quantity <= allocated_quantity),
    UNIQUE (request_id, resource_id),
    FOREIGN KEY (request_id) REFERENCES relief_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE RESTRICT
);

CREATE TABLE verification_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL,
    level TEXT NOT NULL,
    reviewer_id INTEGER NOT NULL,
    decision TEXT NOT NULL CHECK (decision IN ('APPROVED', 'RETURNED', 'REJECTED')),
    reason TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (request_id) REFERENCES relief_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE allocations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL,
    strategy_type TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN (
        'PREVIEW', 'CONFIRMED', 'PARTIALLY_DISPATCHED', 'DISPATCHED', 'CANCELLED', 'COMPLETED'
    )),
    created_by INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    confirmed_at TEXT,
    cancelled_at TEXT,
    FOREIGN KEY (request_id) REFERENCES relief_requests(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE allocation_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    allocation_id INTEGER NOT NULL,
    relief_center_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    dispatched_quantity INTEGER NOT NULL DEFAULT 0 CHECK (dispatched_quantity >= 0),
    delivered_quantity INTEGER NOT NULL DEFAULT 0 CHECK (delivered_quantity >= 0),
    CHECK (dispatched_quantity <= quantity),
    CHECK (delivered_quantity <= dispatched_quantity),
    FOREIGN KEY (allocation_id) REFERENCES allocations(id) ON DELETE CASCADE,
    FOREIGN KEY (relief_center_id) REFERENCES relief_centers(id) ON DELETE RESTRICT,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE RESTRICT
);

CREATE TABLE vehicles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    registration_no TEXT NOT NULL COLLATE NOCASE UNIQUE,
    type TEXT NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    status TEXT NOT NULL CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'IN_TRANSIT', 'UNAVAILABLE')),
    relief_center_id INTEGER,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    FOREIGN KEY (relief_center_id) REFERENCES relief_centers(id) ON DELETE SET NULL
);

CREATE TABLE dispatches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    allocation_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    source_center_id INTEGER NOT NULL,
    destination_area_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('READY', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'CANCELLED')),
    created_at TEXT NOT NULL,
    dispatched_at TEXT,
    delivered_at TEXT,
    failure_reason TEXT,
    FOREIGN KEY (allocation_id) REFERENCES allocations(id) ON DELETE RESTRICT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    FOREIGN KEY (source_center_id) REFERENCES relief_centers(id) ON DELETE RESTRICT,
    FOREIGN KEY (destination_area_id) REFERENCES affected_areas(id) ON DELETE RESTRICT
);

CREATE TABLE dispatch_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dispatch_id INTEGER NOT NULL,
    allocation_item_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    quantity_dispatched INTEGER NOT NULL CHECK (quantity_dispatched > 0),
    quantity_delivered INTEGER NOT NULL DEFAULT 0 CHECK (quantity_delivered >= 0),
    CHECK (quantity_delivered <= quantity_dispatched),
    FOREIGN KEY (dispatch_id) REFERENCES dispatches(id) ON DELETE CASCADE,
    FOREIGN KEY (allocation_item_id) REFERENCES allocation_items(id) ON DELETE RESTRICT,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE RESTRICT
);

CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0, 1)),
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE audit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_user_id INTEGER,
    event_type TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id INTEGER,
    description TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
