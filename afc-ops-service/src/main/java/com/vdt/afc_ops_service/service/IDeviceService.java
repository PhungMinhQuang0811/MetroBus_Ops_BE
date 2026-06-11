package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.device.CreateDeviceRequest;
import com.vdt.afc_ops_service.dto.request.device.UpdateDeviceRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceProvisionResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceResponse;

public interface IDeviceService {
    PageResponse<DeviceResponse> listDevices(Long stationId, String deviceType, String status,
                                             String keyword, int page, int size);

    DeviceDetailResponse getDevice(Long deviceId);

    DeviceProvisionResponse createDevice(CreateDeviceRequest request);

    DeviceResponse updateDevice(Long deviceId, UpdateDeviceRequest request);

    DeviceResponse enableDevice(Long deviceId);

    DeviceResponse disableDevice(Long deviceId);
}
