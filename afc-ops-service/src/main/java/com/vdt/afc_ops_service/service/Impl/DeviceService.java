package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.dto.request.device.CreateDeviceRequest;
import com.vdt.afc_ops_service.dto.request.device.UpdateDeviceRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceProvisionResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.DeviceMapper;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.DeviceCodeGenerator;
import com.vdt.afc_ops_service.service.generator.DeviceSecretGenerator;
import com.vdt.afc_ops_service.service.IDeviceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceService implements IDeviceService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_KEYWORD_LENGTH = 50;

    DeviceRepository deviceRepository;
    StationRepository stationRepository;
    DeviceMapper deviceMapper;
    DeviceCodeGenerator deviceCodeGenerator;
    DeviceSecretGenerator deviceSecretGenerator;
    SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeviceResponse> listDevices(Long stationId, String deviceType, String status,
                                                    String keyword, int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        String normalizedDeviceType = SearchFilterUtil.normalizeUppercase(deviceType);
        String normalizedStatus = SearchFilterUtil.normalizeUppercase(status);
        String normalizedKeyword = SearchFilterUtil.normalize(keyword);
        validateListParams(stationId, normalizedDeviceType, normalizedStatus, normalizedKeyword, page, size);
        if (stationId != null) {
            getStation(stationId, operator);
        }

        Page<Device> devices = deviceRepository.searchDevices(
                operator.getId(),
                stationId,
                normalizedDeviceType,
                normalizedStatus,
                SearchFilterUtil.toKeywordPattern(normalizedKeyword),
                PageRequest.of(page, size)
        );

        return PageResponse.<DeviceResponse>builder()
                .items(devices.getContent().stream().map(deviceMapper::toDeviceResponse).toList())
                .page(devices.getNumber())
                .size(devices.getSize())
                .totalElements(devices.getTotalElements())
                .totalPages(devices.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceDetailResponse getDevice(Long deviceId) {
        Device device = getDevice(deviceId, securityUtils.getRequiredCurrentOperator());
        return deviceMapper.toDetailResponse(device);
    }

    @Override
    @Transactional
    public DeviceProvisionResponse createDevice(CreateDeviceRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Station station = getStation(request.getStationId(), operator);
        String deviceSecret = deviceSecretGenerator.generate();

        Device device = Device.builder()
                .station(station)
                .deviceCode(deviceCodeGenerator.generate(station))
                .deviceType(SearchFilterUtil.normalizeUppercase(request.getDeviceType()))
                .direction(SearchFilterUtil.normalizeUppercase(request.getDirection()))
                .status(PredefinedDeviceStatus.ACTIVE)
                .firmwareVersion(SearchFilterUtil.normalize(request.getFirmwareVersion()))
                .deviceSecret(deviceSecret)
                .createdByAccountId(SecurityUtils.getRequiredCurrentAccountId())
                .build();

        return deviceMapper.toProvisionResponse(deviceRepository.save(device), deviceSecret);
    }

    @Override
    @Transactional
    public DeviceResponse updateDevice(Long deviceId, UpdateDeviceRequest request) {
        validateDeviceId(deviceId);
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Device device = getDevice(deviceId, operator);
        Station station = getStation(request.getStationId(), operator);
        String status = SearchFilterUtil.normalizeUppercase(request.getStatus());
        validateDeviceManagementStatus(status);

        device.setStation(station);
        device.setDeviceType(SearchFilterUtil.normalizeUppercase(request.getDeviceType()));
        device.setDirection(SearchFilterUtil.normalizeUppercase(request.getDirection()));
        device.setStatus(status);
        device.setFirmwareVersion(SearchFilterUtil.normalize(request.getFirmwareVersion()));

        return deviceMapper.toDeviceResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public DeviceResponse enableDevice(Long deviceId) {
        Device device = getDevice(deviceId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedDeviceStatus.ACTIVE.equals(device.getStatus())) {
            throw new AppException(ErrorCode.DEVICE_ALREADY_ENABLED);
        }
        device.setStatus(PredefinedDeviceStatus.ACTIVE);
        return deviceMapper.toDeviceResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public DeviceResponse disableDevice(Long deviceId) {
        Device device = getDevice(deviceId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedDeviceStatus.DISABLED.equals(device.getStatus())) {
            throw new AppException(ErrorCode.DEVICE_ALREADY_DISABLED);
        }
        device.setStatus(PredefinedDeviceStatus.DISABLED);
        return deviceMapper.toDeviceResponse(deviceRepository.save(device));
    }

    private Station getStation(Long stationId, Operator operator) {
        validateStationId(stationId);
        return stationRepository.findByIdAndRouteOperatorId(stationId, operator.getId())
                .orElseGet(() -> {
                    if (stationRepository.existsById(stationId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.STATION_NOT_FOUND);
                });
    }

    private Device getDevice(Long deviceId, Operator operator) {
        validateDeviceId(deviceId);
        return deviceRepository.findByIdAndStationRouteOperatorId(deviceId, operator.getId())
                .orElseGet(() -> {
                    if (deviceRepository.existsById(deviceId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.DEVICE_NOT_FOUND);
                });
    }

    private void validateListParams(Long stationId, String deviceType, String status, String keyword,
                                    int page, int size) {
        if (stationId != null) {
            validateStationId(stationId);
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }
        validateDeviceType(deviceType);
        validateDeviceStatus(status);
    }

    private void validateDeviceType(String deviceType) {
        if (deviceType != null && !PredefinedDeviceType.QR_SCANNER_SIMULATOR.equals(deviceType)) {
            throw new AppException(ErrorCode.INVALID_DEVICE_TYPE);
        }
    }

    private void validateDeviceStatus(String status) {
        if (status != null
                && !PredefinedDeviceStatus.ACTIVE.equals(status)
                && !PredefinedDeviceStatus.OFFLINE.equals(status)
                && !PredefinedDeviceStatus.MAINTENANCE.equals(status)
                && !PredefinedDeviceStatus.DISABLED.equals(status)) {
            throw new AppException(ErrorCode.INVALID_DEVICE_STATUS);
        }
    }

    private void validateDeviceManagementStatus(String status) {
        if (status == null
                || (!PredefinedDeviceStatus.ACTIVE.equals(status)
                && !PredefinedDeviceStatus.MAINTENANCE.equals(status)
                && !PredefinedDeviceStatus.DISABLED.equals(status))) {
            throw new AppException(ErrorCode.INVALID_DEVICE_STATUS);
        }
    }

    private void validateStationId(Long stationId) {
        if (stationId == null || stationId <= 0) {
            throw new AppException(ErrorCode.INVALID_STATION_ID);
        }
    }

    private void validateDeviceId(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new AppException(ErrorCode.INVALID_DEVICE_ID);
        }
    }
}
