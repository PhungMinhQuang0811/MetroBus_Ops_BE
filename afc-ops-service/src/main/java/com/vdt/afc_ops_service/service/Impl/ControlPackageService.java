package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageSourceType;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageStatus;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.dto.request.controlpackage.AckControlPackageApplyRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.PublishControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.UpdateControlPackageRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageAckResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackagePublishResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.PendingControlPackageResponse;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.mapper.ControlPackageMapper;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IControlPackageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ControlPackageService implements IControlPackageService {

    ControlPackageRepository controlPackageRepository;
    ControlPackagePayloadRepository payloadRepository;
    StationControlSyncRepository syncRepository;
    StationRepository stationRepository;
    ControlPackageMapper mapper;
    SecurityUtils securityUtils;

    @Override
    @Transactional
    public ControlPackageResponse create(CreateControlPackageRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        if (!PredefinedControlPackageType.isValid(request.getPackageType())) {
            throw new AppException(ErrorCode.INVALID_CONTROL_PACKAGE_TYPE);
        }

        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CONTROL_PACKAGE_PAYLOAD);
        }

        validatePayload(request.getPackageType(), request.getPayload());

        // Lock operator settings / max version calculation
        Long maxVersion = controlPackageRepository.findMaxVersionForUpdate(operator.getId());
        long version = (maxVersion != null ? maxVersion : 0L) + 1;

        ControlPackage controlPackage = mapper.toEntity(request, operator, version);
        controlPackage = controlPackageRepository.save(controlPackage);

        ControlPackagePayload mongoPayload = mapper.toDocument(request, controlPackage, version);
        mongoPayload = payloadRepository.save(mongoPayload);

        controlPackage.setPayloadRef(mongoPayload.getId());
        controlPackage = controlPackageRepository.save(controlPackage);

        return mapper.toResponse(controlPackage);
    }

    @Override
    @Transactional
    public ControlPackageResponse update(Long packageId, UpdateControlPackageRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ControlPackage controlPackage = controlPackageRepository.findByIdAndOperatorId(packageId, operator.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_NOT_FOUND));

        if (!PredefinedControlPackageStatus.CREATED.equals(controlPackage.getStatus())
                || !PredefinedControlPackageSourceType.LEVEL4_CREATED.equals(controlPackage.getSourceType())) {
            throw new AppException(ErrorCode.CONTROL_PACKAGE_NOT_EDITABLE);
        }

        if (syncRepository.existsByControlPackageId(packageId)) {
            throw new AppException(ErrorCode.CONTROL_PACKAGE_NOT_EDITABLE);
        }

        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CONTROL_PACKAGE_PAYLOAD);
        }

        validatePayload(controlPackage.getPackageType(), request.getPayload());

        ControlPackagePayload mongoPayload = payloadRepository.findByControlPackageId(packageId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND));

        mongoPayload.setPayload(request.getPayload());
        mongoPayload.setUpdatedAt(LocalDateTime.now());
        payloadRepository.save(mongoPayload);

        controlPackage.setUpdatedAt(LocalDateTime.now());
        controlPackageRepository.save(controlPackage);

        return mapper.toResponse(controlPackage);
    }

    @Override
    public ControlPackageDetailResponse getDetail(Long packageId) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ControlPackage controlPackage = controlPackageRepository.findByIdAndOperatorId(packageId, operator.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_NOT_FOUND));

        ControlPackagePayload mongoPayload = payloadRepository.findByControlPackageId(packageId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND));

        return mapper.toDetailResponse(controlPackage, mongoPayload.getPayload());
    }

    @Override
    public PageResponse<ControlPackageResponse> list(String packageType, String sourceType, String status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Pageable pageable = PageRequest.of(page, size, Sort.by("version").descending());

        Page<ControlPackage> pg = controlPackageRepository.searchPackages(
                operator.getId(),
                SearchFilterUtil.normalize(packageType),
                SearchFilterUtil.normalize(sourceType),
                SearchFilterUtil.normalize(status),
                pageable
        );

        return PageResponse.<ControlPackageResponse>builder()
                .items(pg.getContent().stream().map(mapper::toResponse).toList())
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public ControlPackagePublishResponse publish(Long packageId, PublishControlPackageRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ControlPackage controlPackage = controlPackageRepository.findByIdAndOperatorId(packageId, operator.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_NOT_FOUND));

        if (request.getStationIds() == null || request.getStationIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_STATION_LIST);
        }

        if (PredefinedControlPackageStatus.REVOKED.equals(controlPackage.getStatus())) {
            throw new AppException(ErrorCode.CONTROL_PACKAGE_ALREADY_PUBLISHED);
        }

        List<StationControlSync> newSyncs = new ArrayList<>();
        for (Long stationId : request.getStationIds()) {
            Station station = stationRepository.findByIdAndRouteOperatorId(stationId, operator.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

            if (!"ACTIVE".equals(station.getStatus())) {
                throw new AppException(ErrorCode.STATION_ALREADY_DISABLED);
            }

            if (syncRepository.findByStationIdAndControlPackageId(stationId, packageId).isPresent()) {
                continue;
            }

            StationControlSync sync = mapper.toStationControlSync(station, controlPackage);
            newSyncs.add(syncRepository.save(sync));
        }

        if (PredefinedControlPackageStatus.CREATED.equals(controlPackage.getStatus())) {
            controlPackage.setStatus(PredefinedControlPackageStatus.PUBLISHED);
            controlPackage.setPublishedAt(LocalDateTime.now());
            controlPackageRepository.save(controlPackage);
        }

        return mapper.toPublishResponse(controlPackage, newSyncs);
    }

    @Override
    public List<PendingControlPackageResponse> pullPending(String stationCode, Long currentVersion) {
        if (SearchFilterUtil.normalize(stationCode) == null) {
            throw new AppException(ErrorCode.FIELD_REQUIRED);
        }
        if (currentVersion == null) {
            throw new AppException(ErrorCode.INVALID_CONTROL_PACKAGE_VERSION);
        }

        List<StationControlSync> syncs = syncRepository.findAllByStationStationCodeAndSyncStatusAndControlPackageVersionGreaterThanOrderByControlPackageVersionAsc(
                stationCode.trim(),
                "PENDING",
                currentVersion
        );

        List<PendingControlPackageResponse> responses = new ArrayList<>();
        for (StationControlSync sync : syncs) {
            ControlPackagePayload payload = payloadRepository.findByControlPackageId(sync.getControlPackage().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_PAYLOAD_NOT_FOUND));
            responses.add(mapper.toPendingResponse(sync, payload));
        }

        return responses;
    }

    @Override
    @Transactional
    public ControlPackageAckResponse ackApply(Long syncId, AckControlPackageApplyRequest request) {
        if (syncId == null) {
            throw new AppException(ErrorCode.INVALID_CONTROL_SYNC_ID);
        }
        if (request.getSyncStatus() == null || (!"APPLIED".equals(request.getSyncStatus()) && !"FAILED".equals(request.getSyncStatus()))) {
            throw new AppException(ErrorCode.INVALID_CONTROL_SYNC_STATUS);
        }

        StationControlSync sync = syncRepository.findWithRelationsById(syncId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_SYNC_NOT_FOUND));

        sync.setSyncStatus(request.getSyncStatus());
        sync.setRetryCount(sync.getRetryCount() + ("FAILED".equals(request.getSyncStatus()) ? 1 : 0));
        sync.setLastAttemptAt(LocalDateTime.now());
        if ("APPLIED".equals(request.getSyncStatus())) {
            sync.setAppliedAt(LocalDateTime.now());
            sync.setErrorMessage(null);
        } else {
            sync.setErrorMessage(request.getErrorMessage());
        }
        sync.setUpdatedAt(LocalDateTime.now());
        syncRepository.save(sync);

        return mapper.toAckResponse(sync);
    }

    @Override
    public PageResponse<ControlPackageSyncResponse> searchSyncs(String packageType, Long version, Long stationId, String status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<StationControlSync> pg = syncRepository.searchSyncs(
                operator.getId(),
                SearchFilterUtil.normalize(packageType),
                version,
                stationId,
                SearchFilterUtil.normalize(status),
                pageable
        );

        return PageResponse.<ControlPackageSyncResponse>builder()
                .items(pg.getContent().stream().map(mapper::toSyncResponse).toList())
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .build();
    }

    @Override
    public ControlPackageSyncDetailResponse getSyncDetail(Long syncId) {
        if (syncId == null) {
            throw new AppException(ErrorCode.INVALID_CONTROL_SYNC_ID);
        }
        Operator operator = securityUtils.getRequiredCurrentOperator();
        StationControlSync sync = syncRepository.findDetailByIdAndOperatorId(syncId, operator.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTROL_PACKAGE_SYNC_NOT_FOUND));

        return mapper.toSyncDetailResponse(sync);
    }

    private void validatePayload(String packageType, Map<String, Object> payload) {
        if (PredefinedControlPackageType.DEVICE_CONFIG.equals(packageType)) {
            Object deviceTypesObj = payload.get("deviceTypes");
            if (deviceTypesObj != null) {
                if (!(deviceTypesObj instanceof List<?>)) {
                    throw new AppException(ErrorCode.INVALID_DEVICE_TYPE);
                }
                List<?> deviceTypesList = (List<?>) deviceTypesObj;
                for (Object item : deviceTypesList) {
                    if (!(item instanceof String) || !PredefinedDeviceType.QR_SCANNER_SIMULATOR.equals(item)) {
                        throw new AppException(ErrorCode.INVALID_DEVICE_TYPE);
                    }
                }
            }
        }
    }
}
