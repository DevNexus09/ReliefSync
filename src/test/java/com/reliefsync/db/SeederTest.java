package com.reliefsync.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reliefsync.model.ManifestStatus;
import com.reliefsync.model.RequestStatus;
import com.reliefsync.model.VehicleStatus;
import com.reliefsync.repository.DispatchManifestRepository;
import com.reliefsync.repository.RequestRepository;
import com.reliefsync.repository.VehicleRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeederTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        Database.reset();
    }

    @Test
    void freshDemoSeedIsIdempotentAndIncludesTransportManifest() {
        Database.init("jdbc:sqlite:" + tempDir.resolve("seed.db"));
        Seeder.seedDemo();
        Seeder.seedDemo();

        VehicleRepository vehicles = new VehicleRepository();
        assertEquals(2, vehicles.search("").size());
        assertEquals(VehicleStatus.AVAILABLE,
                vehicles.findByRegistration("DHAKA-TRK-01").orElseThrow().status());
        assertEquals(VehicleStatus.MAINTENANCE,
                vehicles.findByRegistration("SYLHET-VAN-02").orElseThrow().status());

        RequestRepository requests = new RequestRepository();
        assertEquals(5, requests.rows("", null).size());
        long deliveredId = requests.rows("", RequestStatus.DELIVERED).getFirst().id();
        assertTrue(new DispatchManifestRepository().findByRequest(deliveredId)
                .filter(manifest -> manifest.status() == ManifestStatus.DELIVERED
                        && manifest.totalLoad() > 0)
                .isPresent());
    }
}
