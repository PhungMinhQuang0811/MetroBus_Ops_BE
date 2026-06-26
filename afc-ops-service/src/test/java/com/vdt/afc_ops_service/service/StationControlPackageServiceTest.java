package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.service.Impl.StationControlPackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationControlPackageServiceTest {

    @Mock
    ControlPackageRepository controlPackageRepository;

    @Mock
    ControlPackagePayloadRepository payloadRepository;

    @Mock
    StationControlSyncRepository syncRepository;

    StationControlPackageService service;

    Operator operator;
    Route route;
    Station station;

    @BeforeEach
    void setUp() {
        service = new StationControlPackageService(controlPackageRepository, payloadRepository, syncRepository);
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder().id(100L).route(route)
                .stationCode("METRO-001-ST-001").stationName("Ben Thanh")
                .stationOrder(1).distance(java.math.BigDecimal.valueOf(1.5))
                .status("ACTIVE").build();
    }

    @Test
    void createOrUpdateStationContext_createsPackageAndSync() {
        when(controlPackageRepository.findMaxVersionForUpdate(1L)).thenReturn(5L);
        when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(payloadRepository.save(any(ControlPackagePayload.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.createOrUpdateStationContext(station);

        ArgumentCaptor<ControlPackage> pkgCaptor = ArgumentCaptor.forClass(ControlPackage.class);
        verify(controlPackageRepository, org.mockito.Mockito.times(2)).save(pkgCaptor.capture());
        ControlPackage saved = pkgCaptor.getValue();
        assertEquals(6L, saved.getVersion());
        assertEquals(PredefinedControlPackageType.STATION_CONTEXT, saved.getPackageType());
        assertEquals("PUBLISHED", saved.getStatus());
        assertEquals(operator, saved.getOperator());

        ArgumentCaptor<StationControlSync> syncCaptor = ArgumentCaptor.forClass(StationControlSync.class);
        verify(syncRepository).save(syncCaptor.capture());
        assertEquals(station, syncCaptor.getValue().getStation());
        assertEquals("PENDING", syncCaptor.getValue().getSyncStatus());
    }

    @Test
    void createOrUpdateStationContext_deletesOldSyncs() {
        when(controlPackageRepository.findMaxVersionForUpdate(1L)).thenReturn(5L);
        when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(payloadRepository.save(any(ControlPackagePayload.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.createOrUpdateStationContext(station);

        verify(syncRepository).deleteByStationIdAndPackageType(
                eq(100L), eq(PredefinedControlPackageType.STATION_CONTEXT));
    }

    @Test
    void createOrUpdateStationContext_createsPayloadWithStationData() {
        when(controlPackageRepository.findMaxVersionForUpdate(1L)).thenReturn(5L);
        when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(payloadRepository.save(any(ControlPackagePayload.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.createOrUpdateStationContext(station);

        ArgumentCaptor<ControlPackagePayload> payloadCaptor = ArgumentCaptor.forClass(ControlPackagePayload.class);
        verify(payloadRepository).save(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue().getPayload();
        assertEquals("METRO-001-ST-001", payload.get("stationCode"));
        assertEquals("Ben Thanh", payload.get("stationName"));
        assertEquals("METRO-001", payload.get("routeCode"));
        assertEquals(1, payload.get("stationOrder"));
        assertNotNull(payload.get("distance"));
        assertEquals("HCMC-METRO", payload.get("operatorCode"));
    }
}