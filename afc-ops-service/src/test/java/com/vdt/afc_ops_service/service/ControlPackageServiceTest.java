package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageSourceType;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageStatus;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.dto.request.controlpackage.AckControlPackageApplyRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.PublishControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.UpdateControlPackageRequest;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.mapper.ControlPackageMapper;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.ControlPackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPackageServiceTest {

    @Mock
    ControlPackageRepository controlPackageRepository;

    @Mock
    ControlPackagePayloadRepository payloadRepository;

    @Mock
    StationControlSyncRepository syncRepository;

    @Mock
    StationRepository stationRepository;

    @Mock
    SecurityUtils securityUtils;

    ControlPackageService service;

    @BeforeEach
    void setUp() {
        service = new ControlPackageService(
                controlPackageRepository,
                payloadRepository,
                syncRepository,
                stationRepository,
                new ControlPackageMapper(),
                securityUtils
        );
        lenient().when(controlPackageRepository.save(any(ControlPackage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_ValidRequest_SavesToMongoAndPostgres() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(controlPackageRepository.findMaxVersionForUpdate(operator.getId())).thenReturn(12L);

        ControlPackagePayload savedPayload = ControlPackagePayload.builder()
                .id("mongo-id-123")
                .controlPackageId(1L)
                .build();
        when(payloadRepository.save(any(ControlPackagePayload.class))).thenReturn(savedPayload);

        CreateControlPackageRequest request = CreateControlPackageRequest.builder()
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .payload(Map.of("maxOfflineSeconds", 60))
                .build();

        var response = service.create(request);

        assertNotNull(response);
        assertEquals(13L, response.getVersion());
        assertEquals(PredefinedControlPackageType.DEVICE_CONFIG, response.getPackageType());
        assertEquals(PredefinedControlPackageStatus.CREATED, response.getStatus());
        verify(controlPackageRepository, org.mockito.Mockito.times(2)).save(any(ControlPackage.class));
        verify(payloadRepository).save(any(ControlPackagePayload.class));
    }

    @Test
    void create_InvalidType_ThrowsException() {
        CreateControlPackageRequest request = CreateControlPackageRequest.builder()
                .packageType("INVALID_TYPE")
                .payload(Map.of("key", "val"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        assertEquals(ErrorCode.INVALID_CONTROL_PACKAGE_TYPE, ex.getErrorCode());
    }

    @Test
    void create_InvalidDeviceTypeInPayload_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        CreateControlPackageRequest request = CreateControlPackageRequest.builder()
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .payload(Map.of("deviceTypes", List.of("INVALID_DEVICE_TYPE")))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        assertEquals(ErrorCode.INVALID_DEVICE_TYPE, ex.getErrorCode());
    }

    @Test
    void create_NonListDeviceTypesInPayload_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        CreateControlPackageRequest request = CreateControlPackageRequest.builder()
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .payload(Map.of("deviceTypes", "INVALID_NOT_A_LIST"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        assertEquals(ErrorCode.INVALID_DEVICE_TYPE, ex.getErrorCode());
    }

    @Test
    void update_InvalidDeviceTypeInPayload_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage draftPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.CREATED)
                .version(13L)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(draftPackage));
        when(syncRepository.existsByControlPackageId(101L)).thenReturn(false);

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("deviceTypes", List.of("INVALID_DEVICE_TYPE")))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(101L, request));
        assertEquals(ErrorCode.INVALID_DEVICE_TYPE, ex.getErrorCode());
    }

    @Test
    void update_DraftAndNotSynced_UpdatesMongoPayload() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage draftPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.CREATED)
                .version(13L)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(draftPackage));
        when(syncRepository.existsByControlPackageId(101L)).thenReturn(false);

        ControlPackagePayload payload = ControlPackagePayload.builder()
                .id("mongo-101")
                .controlPackageId(101L)
                .payload(Map.of("maxOfflineSeconds", 60))
                .build();
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.of(payload));

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("maxOfflineSeconds", 90))
                .build();

        var response = service.update(101L, request);

        assertNotNull(response);
        verify(payloadRepository).save(payload);
        verify(controlPackageRepository).save(draftPackage);
    }

    @Test
    void update_PublishedPackage_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage publishedPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .status(PredefinedControlPackageStatus.PUBLISHED)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(publishedPackage));

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("key", "val"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(101L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_NOT_EDITABLE, ex.getErrorCode());
        verify(payloadRepository, never()).save(any());
    }

    @Test
    void update_SyncedPackage_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage draftPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .status(PredefinedControlPackageStatus.CREATED)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(draftPackage));
        when(syncRepository.existsByControlPackageId(101L)).thenReturn(true);

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("key", "val"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(101L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_NOT_EDITABLE, ex.getErrorCode());
        verify(payloadRepository, never()).save(any());
    }

    @Test
    void getDetail_ValidPackage_ReturnsCombinedMetadataAndPayload() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .status(PredefinedControlPackageStatus.CREATED)
                .version(13L)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));

        ControlPackagePayload payload = ControlPackagePayload.builder()
                .controlPackageId(101L)
                .payload(Map.of("maxOfflineSeconds", 60))
                .build();
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.of(payload));

        var detail = service.getDetail(101L);

        assertNotNull(detail);
        assertEquals(101L, detail.getId());
        assertEquals(60, detail.getPayload().get("maxOfflineSeconds"));
    }

    @Test
    void list_ValidFilters_ReturnsPaginatedMetadataList() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .status(PredefinedControlPackageStatus.CREATED)
                .version(13L)
                .build();
        when(controlPackageRepository.searchPackages(eq(operator.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(controlPackage), Pageable.ofSize(20), 1));

        var response = service.list(PredefinedControlPackageType.DEVICE_CONFIG, null, null, 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(101L, response.getItems().get(0).getId());
    }

    @Test
    void publish_ValidStations_CreatesSyncRecordsAndPublishes() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .status(PredefinedControlPackageStatus.CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));

        Station station = activeStation(50L, "ST-01", "Bến Thành");
        when(stationRepository.findByIdAndRouteOperatorId(50L, operator.getId()))
                .thenReturn(Optional.of(station));

        when(syncRepository.findByStationIdAndControlPackageId(50L, 101L)).thenReturn(Optional.empty());
        when(syncRepository.save(any(StationControlSync.class))).thenAnswer(inv -> {
            StationControlSync scs = inv.getArgument(0);
            scs.setId(888L);
            return scs;
        });

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of(50L))
                .build();

        var response = service.publish(101L, request);

        assertNotNull(response);
        assertEquals(101L, response.getPackageId());
        assertEquals(PredefinedControlPackageStatus.PUBLISHED, response.getStatus());
        assertEquals(1, response.getStationSyncs().size());
        assertEquals(50L, response.getStationSyncs().get(0).getStationId());
        assertEquals("PENDING", response.getStationSyncs().get(0).getSyncStatus());
        assertEquals(PredefinedControlPackageStatus.PUBLISHED, controlPackage.getStatus());
    }

    @Test
    void publish_DisabledStation_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .status(PredefinedControlPackageStatus.CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));

        Station station = activeStation(50L, "ST-01", "Bến Thành");
        station.setStatus("DISABLED");
        when(stationRepository.findByIdAndRouteOperatorId(50L, operator.getId()))
                .thenReturn(Optional.of(station));

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of(50L))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.publish(101L, request));
        assertEquals(ErrorCode.STATION_ALREADY_DISABLED, ex.getErrorCode());
        verify(syncRepository, never()).save(any());
    }

    @Test
    void pullPending_StationWithPendingSyncs_ReturnsPayloads() {
        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .operator(activeOperator())
                .version(13L)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .build();

        StationControlSync sync = StationControlSync.builder()
                .id(888L)
                .station(station)
                .controlPackage(cp)
                .syncStatus("PENDING")
                .build();

        when(syncRepository.findAllByStationStationCodeAndSyncStatusAndControlPackageVersionGreaterThanOrderByControlPackageVersionAsc(
                "ST-01", "PENDING", 12L
        )).thenReturn(List.of(sync));

        ControlPackagePayload payload = ControlPackagePayload.builder()
                .controlPackageId(101L)
                .payload(Map.of("maxOfflineSeconds", 60))
                .build();
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.of(payload));

        var response = service.pullPending("ST-01", 12L);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(888L, response.get(0).getSyncId());
        assertEquals(101L, response.get(0).getPackageId());
        assertEquals(13L, response.get(0).getVersion());
        assertEquals(60, response.get(0).getPayload().get("maxOfflineSeconds"));
    }

    @Test
    void ackApply_AppliedStatus_UpdatesDatabaseRecord() {
        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .operator(activeOperator())
                .version(13L)
                .build();

        StationControlSync sync = StationControlSync.builder()
                .id(888L)
                .station(station)
                .controlPackage(cp)
                .syncStatus("PENDING")
                .retryCount(0)
                .build();

        when(syncRepository.findWithRelationsById(888L)).thenReturn(Optional.of(sync));
        when(syncRepository.save(any(StationControlSync.class))).thenAnswer(inv -> inv.getArgument(0));

        AckControlPackageApplyRequest request = AckControlPackageApplyRequest.builder()
                .syncStatus("APPLIED")
                .build();

        var response = service.ackApply(888L, request);

        assertNotNull(response);
        assertEquals(888L, response.getSyncId());
        assertEquals("APPLIED", response.getSyncStatus());
        assertEquals("APPLIED", sync.getSyncStatus());
        assertNotNull(sync.getAppliedAt());
        verify(syncRepository).save(sync);
    }

    @Test
    void ackApply_FailedStatus_IncrementsRetryCountAndSavesError() {
        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .operator(activeOperator())
                .version(13L)
                .build();

        StationControlSync sync = StationControlSync.builder()
                .id(888L)
                .station(station)
                .controlPackage(cp)
                .syncStatus("PENDING")
                .retryCount(1)
                .build();

        when(syncRepository.findWithRelationsById(888L)).thenReturn(Optional.of(sync));
        when(syncRepository.save(any(StationControlSync.class))).thenAnswer(inv -> inv.getArgument(0));

        AckControlPackageApplyRequest request = AckControlPackageApplyRequest.builder()
                .syncStatus("FAILED")
                .errorMessage("MongoDB Connection Error")
                .build();

        var response = service.ackApply(888L, request);

        assertNotNull(response);
        assertEquals(888L, response.getSyncId());
        assertEquals("FAILED", response.getSyncStatus());
        assertEquals("FAILED", sync.getSyncStatus());
        assertEquals(2, sync.getRetryCount());
        assertEquals("MongoDB Connection Error", sync.getErrorMessage());
        verify(syncRepository).save(sync);
    }

    @Test
    void searchSyncs_ValidFilters_ReturnsSyncList() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .version(13L)
                .build();

        StationControlSync sync = StationControlSync.builder()
                .id(888L)
                .station(station)
                .controlPackage(cp)
                .syncStatus("PENDING")
                .build();

        when(syncRepository.searchSyncs(eq(operator.getId()), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sync), Pageable.ofSize(20), 1));

        var response = service.searchSyncs(PredefinedControlPackageType.DEVICE_CONFIG, null, null, null, 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(888L, response.getItems().get(0).getSyncId());
        assertEquals("ST-01", response.getItems().get(0).getStationCode());
    }

    @Test
    void getSyncDetail_ValidId_ReturnsSyncStateAndPackageMetadata() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .version(13L)
                .status(PredefinedControlPackageStatus.PUBLISHED)
                .build();

        StationControlSync sync = StationControlSync.builder()
                .id(888L)
                .station(station)
                .controlPackage(cp)
                .syncStatus("FAILED")
                .retryCount(2)
                .errorMessage("Connection failure")
                .build();

        when(syncRepository.findDetailByIdAndOperatorId(888L, operator.getId())).thenReturn(Optional.of(sync));

        var response = service.getSyncDetail(888L);

        assertNotNull(response);
        assertEquals(888L, response.getSyncId());
        assertEquals("ST-01", response.getStationCode());
        assertEquals("Connection failure", response.getErrorMessage());
        assertEquals("PUBLISHED", response.getPackageStatus());
    }

    @Test
    void create_EmptyPayload_ThrowsException() {
        CreateControlPackageRequest request = CreateControlPackageRequest.builder()
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .payload(Map.of())
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        assertEquals(ErrorCode.INVALID_CONTROL_PACKAGE_PAYLOAD, ex.getErrorCode());
    }

    @Test
    void update_PackageNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(controlPackageRepository.findByIdAndOperatorId(999L, operator.getId()))
                .thenReturn(Optional.empty());

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("key", "val"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(999L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void update_EmptyPayload_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage draftPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(draftPackage));
        when(syncRepository.existsByControlPackageId(101L)).thenReturn(false);

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of())
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(101L, request));
        assertEquals(ErrorCode.INVALID_CONTROL_PACKAGE_PAYLOAD, ex.getErrorCode());
    }

    @Test
    void update_PayloadNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage draftPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .packageType(PredefinedControlPackageType.DEVICE_CONFIG)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.CREATED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(draftPackage));
        when(syncRepository.existsByControlPackageId(101L)).thenReturn(false);
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.empty());

        UpdateControlPackageRequest request = UpdateControlPackageRequest.builder()
                .payload(Map.of("key", "val"))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.update(101L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getDetail_PackageNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(controlPackageRepository.findByIdAndOperatorId(999L, operator.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getDetail(999L));
        assertEquals(ErrorCode.CONTROL_PACKAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getDetail_PayloadNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getDetail(101L));
        assertEquals(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void list_InvalidPageParams_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> service.list(null, null, null, -1, 20));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());

        ex = assertThrows(AppException.class, () -> service.list(null, null, null, 0, 0));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());

        ex = assertThrows(AppException.class, () -> service.list(null, null, null, 0, 101));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());
    }

    @Test
    void publish_PackageNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(controlPackageRepository.findByIdAndOperatorId(999L, operator.getId()))
                .thenReturn(Optional.empty());

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of(1L))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.publish(999L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void publish_InvalidStationList_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of())
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.publish(101L, request));
        assertEquals(ErrorCode.INVALID_STATION_LIST, ex.getErrorCode());
    }

    @Test
    void publish_StationNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));
        when(stationRepository.findByIdAndRouteOperatorId(50L, operator.getId()))
                .thenReturn(Optional.empty());

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of(50L))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.publish(101L, request));
        assertEquals(ErrorCode.STATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void publish_StationSyncAlreadyExists_SkipSaving() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ControlPackage controlPackage = ControlPackage.builder()
                .id(101L)
                .operator(operator)
                .status(PredefinedControlPackageStatus.PUBLISHED)
                .build();
        when(controlPackageRepository.findByIdAndOperatorId(101L, operator.getId()))
                .thenReturn(Optional.of(controlPackage));

        Station station = activeStation(50L, "ST-01", "Bến Thành");
        when(stationRepository.findByIdAndRouteOperatorId(50L, operator.getId()))
                .thenReturn(Optional.of(station));
        when(syncRepository.findByStationIdAndControlPackageId(50L, 101L))
                .thenReturn(Optional.of(StationControlSync.builder().build()));

        PublishControlPackageRequest request = PublishControlPackageRequest.builder()
                .stationIds(List.of(50L))
                .build();

        var response = service.publish(101L, request);

        assertNotNull(response);
        assertEquals(0, response.getStationSyncs().size());
        verify(syncRepository, never()).save(any());
    }

    @Test
    void pullPending_EmptyStationCode_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> service.pullPending("", 12L));
        assertEquals(ErrorCode.FIELD_REQUIRED, ex.getErrorCode());
    }

    @Test
    void pullPending_NullVersion_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> service.pullPending("ST-01", null));
        assertEquals(ErrorCode.INVALID_CONTROL_PACKAGE_VERSION, ex.getErrorCode());
    }

    @Test
    void pullPending_PayloadNotFoundForSync_ThrowsException() {
        Station station = activeStation(50L, "ST-01", "Bến Thành");
        ControlPackage cp = ControlPackage.builder()
                .id(101L)
                .build();
        StationControlSync sync = StationControlSync.builder()
                .station(station)
                .controlPackage(cp)
                .build();

        when(syncRepository.findAllByStationStationCodeAndSyncStatusAndControlPackageVersionGreaterThanOrderByControlPackageVersionAsc(
                "ST-01", "PENDING", 12L
        )).thenReturn(List.of(sync));
        when(payloadRepository.findByControlPackageId(101L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.pullPending("ST-01", 12L));
        assertEquals(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void ackApply_NullSyncId_ThrowsException() {
        AckControlPackageApplyRequest request = AckControlPackageApplyRequest.builder()
                .syncStatus("APPLIED")
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.ackApply(null, request));
        assertEquals(ErrorCode.INVALID_CONTROL_SYNC_ID, ex.getErrorCode());
    }

    @Test
    void ackApply_InvalidStatus_ThrowsException() {
        AckControlPackageApplyRequest request = AckControlPackageApplyRequest.builder()
                .syncStatus("INVALID")
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.ackApply(500L, request));
        assertEquals(ErrorCode.INVALID_CONTROL_SYNC_STATUS, ex.getErrorCode());
    }

    @Test
    void ackApply_SyncRecordNotFound_ThrowsException() {
        AckControlPackageApplyRequest request = AckControlPackageApplyRequest.builder()
                .syncStatus("APPLIED")
                .build();
        when(syncRepository.findWithRelationsById(500L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.ackApply(500L, request));
        assertEquals(ErrorCode.CONTROL_PACKAGE_SYNC_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void searchSyncs_InvalidPageParams_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> service.searchSyncs(null, null, null, null, -1, 20));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());
    }

    @Test
    void getSyncDetail_NullSyncId_ThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> service.getSyncDetail(null));
        assertEquals(ErrorCode.INVALID_CONTROL_SYNC_ID, ex.getErrorCode());
    }

    @Test
    void getSyncDetail_SyncNotFound_ThrowsException() {
        Operator operator = activeOperator();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(syncRepository.findDetailByIdAndOperatorId(500L, operator.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getSyncDetail(500L));
        assertEquals(ErrorCode.CONTROL_PACKAGE_SYNC_NOT_FOUND, ex.getErrorCode());
    }

    private Operator activeOperator() {
        return Operator.builder()
                .id(1L)
                .operatorCode("METRO-01")
                .operatorName("Metro HCMC")
                .status("ACTIVE")
                .build();
    }

    private Station activeStation(Long id, String code, String name) {
        Route r = Route.builder()
                .id(10L)
                .operator(activeOperator())
                .routeCode("METRO-L1")
                .routeName("Line 1")
                .build();
        return Station.builder()
                .id(id)
                .stationCode(code)
                .stationName(name)
                .route(r)
                .status("ACTIVE")
                .build();
    }
}
