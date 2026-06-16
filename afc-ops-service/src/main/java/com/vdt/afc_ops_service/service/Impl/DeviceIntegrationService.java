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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceIntegrationService implements IDeviceIntegrationService {

    DeviceRepository deviceRepository;
    DeviceHeartbeatRepository deviceHeartbeatRepository;
    DeviceIncidentRepository deviceIncidentRepository;

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
}
