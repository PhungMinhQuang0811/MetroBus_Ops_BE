package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.service.Impl.MediaAccessRulePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAccessRulePackageServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    StationRepository stationRepository;

    @Mock
    ControlPackageRepository controlPackageRepository;

    @Mock
    ControlPackagePayloadRepository payloadRepository;

    @Mock
    StationControlSyncRepository syncRepository;

    MediaAccessRulePackageService service;

    Operator operator;
    Route route;
    Station station;

    @BeforeEach
    void setUp() {
        service = new MediaAccessRulePackageService(cardRepository, stationRepository,
                controlPackageRepository, payloadRepository, syncRepository);
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder().id(100L).route(route)
                .stationCode("METRO-001-ST-001").status("ACTIVE").build();
    }

    @Test
    void refreshAndPublishForOperator_hasBlockedCards_createsPackageAndSyncs() {
        List<Card> blockedCards = List.of(
                Card.builder().id("CARD-001").status("BLACKLISTED").statusReason("LOST_CARD").build(),
                Card.builder().id("CARD-002").status("CANCELLED").statusReason("FRAUD").build()
        );
        when(cardRepository.findByStatusIn(List.of("BLACKLISTED", "CANCELLED"))).thenReturn(blockedCards);
        when(controlPackageRepository.findMaxVersionForUpdate(1L)).thenReturn(5L);
        when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(payloadRepository.save(any(ControlPackagePayload.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(stationRepository.findAllByStatusAndRouteOperatorId("ACTIVE", 1L))
                .thenReturn(List.of(station));

        service.refreshAndPublishForOperator(operator);

        ArgumentCaptor<ControlPackage> pkgCaptor = ArgumentCaptor.forClass(ControlPackage.class);
        verify(controlPackageRepository, org.mockito.Mockito.times(2)).save(pkgCaptor.capture());
        ControlPackage saved = pkgCaptor.getValue();
        assertEquals(6L, saved.getVersion());
        assertEquals(PredefinedControlPackageType.MEDIA_ACCESS_RULES, saved.getPackageType());

        verify(syncRepository).deleteByStationIdAndPackageType(
                eq(100L), eq(PredefinedControlPackageType.MEDIA_ACCESS_RULES));
        ArgumentCaptor<StationControlSync> syncCaptor = ArgumentCaptor.forClass(StationControlSync.class);
        verify(syncRepository).save(syncCaptor.capture());
        assertEquals("PENDING", syncCaptor.getValue().getSyncStatus());
    }

    @Test
    void refreshAndPublishForOperator_noBlockedCards_skips() {
        when(cardRepository.findByStatusIn(List.of("BLACKLISTED", "CANCELLED"))).thenReturn(List.of());

        service.refreshAndPublishForOperator(operator);

        verify(controlPackageRepository, never()).save(any(ControlPackage.class));
        verify(syncRepository, never()).save(any(StationControlSync.class));
    }

    @Test
    void refreshAndPublishForOperator_publishesToAllActiveStations() {
        Station station2 = Station.builder().id(200L).route(route)
                .stationCode("METRO-001-ST-002").status("ACTIVE").build();
        List<Card> blockedCards = List.of(
                Card.builder().id("CARD-001").status("BLACKLISTED").statusReason("LOST").build()
        );
        when(cardRepository.findByStatusIn(List.of("BLACKLISTED", "CANCELLED"))).thenReturn(blockedCards);
        when(controlPackageRepository.findMaxVersionForUpdate(1L)).thenReturn(3L);
        when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(payloadRepository.save(any(ControlPackagePayload.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(stationRepository.findAllByStatusAndRouteOperatorId("ACTIVE", 1L))
                .thenReturn(List.of(station, station2));

        service.refreshAndPublishForOperator(operator);

        // Should delete old syncs for both stations
        verify(syncRepository).deleteByStationIdAndPackageType(eq(100L), eq(PredefinedControlPackageType.MEDIA_ACCESS_RULES));
        verify(syncRepository).deleteByStationIdAndPackageType(eq(200L), eq(PredefinedControlPackageType.MEDIA_ACCESS_RULES));
        // Should save sync for both stations
        ArgumentCaptor<StationControlSync> syncCaptor = ArgumentCaptor.forClass(StationControlSync.class);
        verify(syncRepository, org.mockito.Mockito.times(2)).save(syncCaptor.capture());
    }
}