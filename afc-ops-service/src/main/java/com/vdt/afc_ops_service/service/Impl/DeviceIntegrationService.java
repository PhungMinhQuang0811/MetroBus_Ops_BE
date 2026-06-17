package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.document.DeviceHeartbeat;
import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceHeartbeatRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceIncidentRepository;
import com.vdt.afc_ops_service.service.IDeviceIntegrationService;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceHeartbeatHistoryResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceStatusResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentDetailResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceIntegrationService implements IDeviceIntegrationService {

    DeviceRepository deviceRepository;
    DeviceHeartbeatRepository deviceHeartbeatRepository;
    DeviceIncidentRepository deviceIncidentRepository;
    StationRepository stationRepository;
    SecurityUtils securityUtils;
    MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public SubmitHeartbeatResponse submitHeartbeat(String deviceCode, String deviceSecret, SubmitHeartbeatRequest request) {
        Device device = authenticateDevice(deviceCode, deviceSecret);

        // Update Postgres device status
        device.setLastSeenAt(LocalDateTime.now());
        device.setFirmwareVersion(request.getFirmwareVersion());
        
        // Only update status to the reported status if it's not MAINTENANCE or DISABLED by admin
        if (!"MAINTENANCE".equalsIgnoreCase(device.getStatus()) && !"DISABLED".equalsIgnoreCase(device.getStatus())) {
            device.setStatus(request.getStatus().trim().toUpperCase());
        }
        deviceRepository.save(device);

        // Save history to MongoDB
        DeviceHeartbeat heartbeat = DeviceHeartbeat.builder()
                .operatorId(device.getStation().getRoute().getOperator().getId())
                .deviceId(device.getId())
                .deviceCode(device.getDeviceCode())
                .stationId(device.getStation().getId())
                .status(request.getStatus().trim().toUpperCase())
                .firmwareVersion(request.getFirmwareVersion().trim())
                .sentAt(request.getSentAt())
                .receivedAt(LocalDateTime.now())
                .payload(request.getMetrics())
                .build();
        deviceHeartbeatRepository.save(heartbeat);

        log.info("Successfully recorded heartbeat for device code: {}", deviceCode);

        return SubmitHeartbeatResponse.builder()
                .deviceCode(deviceCode)
                .accepted(true)
                .serverTime(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public SubmitIncidentResponse submitIncident(String deviceCode, String deviceSecret, SubmitIncidentRequest request) {
        Device device = authenticateDevice(deviceCode, deviceSecret);

        // Save incident to MongoDB
        DeviceIncident incident = DeviceIncident.builder()
                .operatorId(device.getStation().getRoute().getOperator().getId())
                .deviceId(device.getId())
                .deviceCode(device.getDeviceCode())
                .stationId(device.getStation().getId())
                .incidentType(request.getIncidentType().trim())
                .severity(request.getSeverity().trim().toUpperCase())
                .message(request.getMessage() != null ? request.getMessage().trim() : null)
                .occurredAt(request.getOccurredAt())
                .receivedAt(LocalDateTime.now())
                .payload(request.getPayload())
                .build();
        DeviceIncident savedIncident = deviceIncidentRepository.save(incident);

        log.info("Successfully recorded incident for device code: {}, incident type: {}", deviceCode, request.getIncidentType());

        return SubmitIncidentResponse.builder()
                .accepted(true)
                .incidentId(savedIncident.getId())
                .build();
    }

    private Device authenticateDevice(String deviceCode, String deviceSecret) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));

        if (device.getDeviceSecret() == null || !device.getDeviceSecret().equals(deviceSecret)) {
            throw new AppException(ErrorCode.DEVICE_AUTH_FAILED);
        }

        return device;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeviceStatusResponse> getDeviceStatus(Long routeId, Long stationId, String status, int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (stationId != null) {
            stationRepository.findByIdAndRouteOperatorId(stationId, operator.getId())
                    .orElseThrow(() -> {
                        if (stationRepository.existsById(stationId)) {
                            return new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                        }
                        return new AppException(ErrorCode.STATION_NOT_FOUND);
                    });
        }

        String normalizedStatus = status != null ? status.trim().toUpperCase() : null;
        if (normalizedStatus != null
                && !"ACTIVE".equals(normalizedStatus)
                && !"OFFLINE".equals(normalizedStatus)
                && !"MAINTENANCE".equals(normalizedStatus)
                && !"DISABLED".equals(normalizedStatus)) {
            throw new AppException(ErrorCode.INVALID_DEVICE_STATUS);
        }

        Page<Device> devices = deviceRepository.searchDeviceStatus(
                operator.getId(),
                routeId,
                stationId,
                normalizedStatus,
                PageRequest.of(page, size)
        );

        LocalDateTime now = LocalDateTime.now();

        return PageResponse.<DeviceStatusResponse>builder()
                .items(devices.getContent().stream().map(device -> {
                    Long offlineSeconds = null;
                    if (device.getLastSeenAt() != null) {
                        offlineSeconds = Duration.between(device.getLastSeenAt(), now).toSeconds();
                        if (offlineSeconds < 0) {
                            offlineSeconds = 0L;
                        }
                    }
                    return DeviceStatusResponse.builder()
                            .deviceId(device.getId())
                            .deviceCode(device.getDeviceCode())
                            .stationId(device.getStation().getId())
                            .stationName(device.getStation().getStationName())
                            .deviceType(device.getDeviceType())
                            .status(device.getStatus())
                            .lastSeenAt(device.getLastSeenAt())
                            .offlineSeconds(offlineSeconds)
                            .build();
                }).toList())
                .page(devices.getNumber())
                .size(devices.getSize())
                .totalElements(devices.getTotalElements())
                .totalPages(devices.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeviceHeartbeatHistoryResponse> getDeviceHeartbeats(Long deviceId, int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        Device device = deviceRepository.findByIdAndStationRouteOperatorId(deviceId, operator.getId())
                .orElseThrow(() -> {
                    if (deviceRepository.existsById(deviceId)) {
                        return new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    return new AppException(ErrorCode.DEVICE_NOT_FOUND);
                });

        Page<DeviceHeartbeat> heartbeats = deviceHeartbeatRepository.findAllByDeviceId(
                device.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"))
        );

        return PageResponse.<DeviceHeartbeatHistoryResponse>builder()
                .items(heartbeats.getContent().stream().map(hb -> DeviceHeartbeatHistoryResponse.builder()
                        .id(hb.getId())
                        .deviceId(hb.getDeviceId())
                        .deviceCode(hb.getDeviceCode())
                        .stationId(hb.getStationId())
                        .status(hb.getStatus())
                        .firmwareVersion(hb.getFirmwareVersion())
                        .sentAt(hb.getSentAt())
                        .receivedAt(hb.getReceivedAt())
                        .payload(hb.getPayload())
                        .build()
                ).toList())
                .page(heartbeats.getNumber())
                .size(heartbeats.getSize())
                .totalElements(heartbeats.getTotalElements())
                .totalPages(heartbeats.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeviceIncidentResponse> searchIncidents(
            LocalDateTime from, LocalDateTime to, Long stationId, Long deviceId,
            String severity, String incidentType, Boolean resolved, int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        Criteria criteria = Criteria.where("operatorId").is(operator.getId());

        if (from != null && to != null) {
            criteria.and("occurredAt").gte(from).lte(to);
        } else if (from != null) {
            criteria.and("occurredAt").gte(from);
        } else if (to != null) {
            criteria.and("occurredAt").lte(to);
        }

        if (stationId != null) {
            criteria.and("stationId").is(stationId);
        }

        if (deviceId != null) {
            criteria.and("deviceId").is(deviceId);
        }

        if (severity != null && !severity.trim().isEmpty()) {
            criteria.and("severity").is(severity.trim().toUpperCase());
        }

        if (incidentType != null && !incidentType.trim().isEmpty()) {
            criteria.and("incidentType").is(incidentType.trim().toUpperCase());
        }

        if (resolved != null) {
            if (resolved) {
                criteria.and("resolvedAt").ne(null);
            } else {
                criteria.and("resolvedAt").is(null);
            }
        }

        Query query = new Query(criteria);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        query.with(pageable);

        List<DeviceIncident> list = mongoTemplate.find(query, DeviceIncident.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), DeviceIncident.class);

        Page<DeviceIncident> incidentPage = PageableExecutionUtils.getPage(list, pageable, () -> total);

        return PageResponse.<DeviceIncidentResponse>builder()
                .items(incidentPage.getContent().stream().map(inc -> DeviceIncidentResponse.builder()
                        .id(inc.getId())
                        .deviceId(inc.getDeviceId())
                        .deviceCode(inc.getDeviceCode())
                        .stationId(inc.getStationId())
                        .incidentType(inc.getIncidentType())
                        .severity(inc.getSeverity())
                        .message(inc.getMessage())
                        .occurredAt(inc.getOccurredAt())
                        .receivedAt(inc.getReceivedAt())
                        .resolvedAt(inc.getResolvedAt())
                        .build()
                ).toList())
                .page(incidentPage.getNumber())
                .size(incidentPage.getSize())
                .totalElements(incidentPage.getTotalElements())
                .totalPages(incidentPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceIncidentDetailResponse getIncidentDetail(String incidentId) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        DeviceIncident incident = deviceIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        if (!operator.getId().equals(incident.getOperatorId())) {
            throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
        }

        Device device = deviceRepository.findById(incident.getDeviceId())
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));

        return DeviceIncidentDetailResponse.builder()
                .id(incident.getId())
                .deviceId(incident.getDeviceId())
                .deviceCode(incident.getDeviceCode())
                .deviceType(device.getDeviceType())
                .deviceStatus(device.getStatus())
                .stationId(device.getStation().getId())
                .stationCode(device.getStation().getStationCode())
                .stationName(device.getStation().getStationName())
                .routeId(device.getStation().getRoute().getId())
                .routeCode(device.getStation().getRoute().getRouteCode())
                .routeName(device.getStation().getRoute().getRouteName())
                .incidentType(incident.getIncidentType())
                .severity(incident.getSeverity())
                .message(incident.getMessage())
                .occurredAt(incident.getOccurredAt())
                .receivedAt(incident.getReceivedAt())
                .resolvedAt(incident.getResolvedAt())
                .payload(incident.getPayload())
                .build();
    }
}
