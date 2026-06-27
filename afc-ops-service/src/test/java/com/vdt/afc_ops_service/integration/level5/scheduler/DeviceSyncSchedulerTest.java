package com.vdt.afc_ops_service.integration.level5.scheduler;

import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.messaging.ControlPackagePublisher;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSyncSchedulerTest {

    @Mock
    StationRepository stationRepository;

    @Mock
    StationControlSyncRepository syncRepository;

    @Mock
    ControlPackagePayloadRepository payloadRepository;

    @Mock
    ControlPackagePublisher controlPackagePublisher;

    DeviceSyncScheduler scheduler;

    Operator operator;
    Route route;
    Station station;

    @BeforeEach
    void setUp() {
        scheduler = new DeviceSyncScheduler(stationRepository, syncRepository,
                payloadRepository, controlPackagePublisher);
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder().id(100L).route(route)
                .stationCode("METRO-001-ST-001").stationName("Ben Thanh")
                .stationOrder(1).distance(java.math.BigDecimal.valueOf(1.5))
                .status("ACTIVE").build();
    }

    @Test
    void syncAllDevices_activeStations_publishesCombinedPayload() {
        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station));
        when(syncRepository.findByStationAndStatus(station.getStationCode(),
                List.of("PENDING", "APPLIED"))).thenReturn(List.of());

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertNotNull(payload.get("publishedAt"));
        Map<String, Object> ctx = (Map<String, Object>) payload.get("stationContext");
        assertNotNull(ctx);
        assertEquals("METRO-001-ST-001", ctx.get("stationCode"));
        assertEquals("Ben Thanh", ctx.get("stationName"));
    }

    @Test
    void syncAllDevices_noActiveStations_skips() {
        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of());

        scheduler.syncAllDevices();

        verify(controlPackagePublisher, never()).publishToStation(anyString(), any());
    }

    @Test
    void syncAllDevices_syncsFound_usesPayloadFromMongo() {
        ControlPackage cp1 = ControlPackage.builder().id(1L).packageType("DEVICE_CONFIG").version(5L).build();
        ControlPackage cp2 = ControlPackage.builder().id(2L).packageType("MEDIA_ACCESS_RULES").version(3L).build();

        StationControlSync sync1 = StationControlSync.builder().station(station).controlPackage(cp1)
                .syncStatus("PENDING").retryCount(0).build();
        StationControlSync sync2 = StationControlSync.builder().station(station).controlPackage(cp2)
                .syncStatus("APPLIED").retryCount(0).build();

        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station));
        when(syncRepository.findByStationAndStatus(station.getStationCode(),
                List.of("PENDING", "APPLIED"))).thenReturn(List.of(sync1, sync2));
        when(payloadRepository.findByControlPackageId(1L)).thenReturn(
                Optional.of(ControlPackagePayload.builder().payload(Map.of("maxOfflineSeconds", 60)).build()));
        when(payloadRepository.findByControlPackageId(2L)).thenReturn(
                Optional.of(ControlPackagePayload.builder().payload(Map.of("cardStatusRules", List.of())).build()));

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertNotNull(payload.get("deviceConfig"));
        assertNotNull(payload.get("mediaAccessRules"));
    }

    @Test
    void syncAllDevices_noSyncs_usesStationContextFallbackOnly() {
        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station));
        when(syncRepository.findByStationAndStatus(station.getStationCode(),
                List.of("PENDING", "APPLIED"))).thenReturn(List.of());

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        // Only stationContext (fallback), no deviceConfig/mediaAccessRules
        assertNotNull(payload.get("stationContext"));
        assertTrue(payload.get("deviceConfig") == null || payload.get("deviceConfig") == null);
    }

    @Test
    void syncAllDevices_publishException_logsErrorAndContinues() {
        Station station2 = Station.builder().id(200L).route(route)
                .stationCode("METRO-001-ST-002").stationName("Opera")
                .status("ACTIVE").build();

        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station, station2));
        when(syncRepository.findByStationAndStatus(anyString(), any()))
                .thenReturn(List.of());
        // Simulate exception for first station
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), any());
        // Second station succeeds

        scheduler.syncAllDevices();

        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), any());
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-002"), any());
    }

    @Test
    void syncAllDevices_nullFieldsInStationContext_fallbackUsesDefaultValues() {
        Station nullFieldStation = Station.builder()
                .id(300L)
                .stationCode("METRO-001-ST-003")
                .stationName(null)
                .route(null) // this will also check operatorCode fallback
                .distance(null)
                .status("ACTIVE")
                .build();

        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(nullFieldStation));
        when(syncRepository.findByStationAndStatus("METRO-001-ST-003", List.of("PENDING", "APPLIED")))
                .thenReturn(List.of());

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-003"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        Map<String, Object> ctx = (Map<String, Object>) payload.get("stationContext");
        assertNotNull(ctx);
        assertEquals("", ctx.get("stationName"));
        assertEquals("", ctx.get("routeCode"));
        assertEquals(java.math.BigDecimal.ZERO, ctx.get("distance"));
        assertEquals("", ctx.get("operatorCode"));
    }

    @Test
    void syncAllDevices_emptyPayloadDoc_doesNotAddPayload() {
        ControlPackage cp = ControlPackage.builder().id(1L).packageType("DEVICE_CONFIG").version(5L).build();
        StationControlSync sync = StationControlSync.builder().station(station).controlPackage(cp)
                .syncStatus("PENDING").retryCount(0).build();

        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station));
        when(syncRepository.findByStationAndStatus(station.getStationCode(), List.of("PENDING", "APPLIED")))
                .thenReturn(List.of(sync));
        when(payloadRepository.findByControlPackageId(1L)).thenReturn(Optional.empty());

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        // Since loadPayloadFromSync returned null because document was empty, deviceConfig should not be in the payload
        assertTrue(!payload.containsKey("deviceConfig"));
        assertNotNull(payload.get("stationContext"));
    }

    @Test
    void syncAllDevices_multipleSyncPackages_ignoresDuplicatesAndUsesContextFromDb() {
        ControlPackage cpDc1 = ControlPackage.builder().id(10L).packageType("DEVICE_CONFIG").version(10L).build();
        ControlPackage cpDc2 = ControlPackage.builder().id(11L).packageType("DEVICE_CONFIG").version(11L).build();
        ControlPackage cpSc1 = ControlPackage.builder().id(20L).packageType("STATION_CONTEXT").version(20L).build();
        ControlPackage cpSc2 = ControlPackage.builder().id(21L).packageType("STATION_CONTEXT").version(21L).build();
        ControlPackage cpMr1 = ControlPackage.builder().id(30L).packageType("MEDIA_ACCESS_RULES").version(30L).build();
        ControlPackage cpMr2 = ControlPackage.builder().id(31L).packageType("MEDIA_ACCESS_RULES").version(31L).build();

        StationControlSync syncDc1 = StationControlSync.builder().station(station).controlPackage(cpDc1).syncStatus("PENDING").build();
        StationControlSync syncDc2 = StationControlSync.builder().station(station).controlPackage(cpDc2).syncStatus("PENDING").build();
        StationControlSync syncSc1 = StationControlSync.builder().station(station).controlPackage(cpSc1).syncStatus("PENDING").build();
        StationControlSync syncSc2 = StationControlSync.builder().station(station).controlPackage(cpSc2).syncStatus("PENDING").build();
        StationControlSync syncMr1 = StationControlSync.builder().station(station).controlPackage(cpMr1).syncStatus("PENDING").build();
        StationControlSync syncMr2 = StationControlSync.builder().station(station).controlPackage(cpMr2).syncStatus("PENDING").build();

        when(stationRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(station));
        when(syncRepository.findByStationAndStatus(station.getStationCode(), List.of("PENDING", "APPLIED")))
                .thenReturn(List.of(syncDc1, syncDc2, syncSc1, syncSc2, syncMr1, syncMr2));

        ControlPackagePayload docDc = ControlPackagePayload.builder().payload(Map.of("param1", "value1")).build();
        when(payloadRepository.findByControlPackageId(10L)).thenReturn(Optional.of(docDc));

        ControlPackagePayload docSc = ControlPackagePayload.builder().payload(Map.of("param2", "value2")).build();
        when(payloadRepository.findByControlPackageId(20L)).thenReturn(Optional.of(docSc));

        ControlPackagePayload docMr = ControlPackagePayload.builder().payload(Map.of("param3", "value3")).build();
        when(payloadRepository.findByControlPackageId(30L)).thenReturn(Optional.of(docMr));

        scheduler.syncAllDevices();

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(controlPackagePublisher).publishToStation(eq("METRO-001-ST-001"), payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        verify(payloadRepository, never()).findByControlPackageId(11L);
        verify(payloadRepository, never()).findByControlPackageId(21L);
        verify(payloadRepository, never()).findByControlPackageId(31L);

        assertEquals(Map.of("param1", "value1", "version", 10L), payload.get("deviceConfig"));
        assertEquals(Map.of("param2", "value2", "version", 20L), payload.get("stationContext"));
        assertEquals(Map.of("param3", "value3", "version", 30L), payload.get("mediaAccessRules"));
    }
}