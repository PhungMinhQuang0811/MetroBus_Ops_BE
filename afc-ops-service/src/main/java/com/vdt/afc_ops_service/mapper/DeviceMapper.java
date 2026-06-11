package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.device.DeviceDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceProvisionResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toDeviceResponse(Device device) {
        Station station = device.getStation();
        Route route = station.getRoute();
        return DeviceResponse.builder()
                .id(device.getId())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType())
                .direction(device.getDirection())
                .status(device.getStatus())
                .firmwareVersion(device.getFirmwareVersion())
                .lastSeenAt(device.getLastSeenAt())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    public DeviceProvisionResponse toProvisionResponse(Device device, String deviceSecret) {
        Station station = device.getStation();
        Route route = station.getRoute();
        return DeviceProvisionResponse.builder()
                .id(device.getId())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType())
                .direction(device.getDirection())
                .status(device.getStatus())
                .firmwareVersion(device.getFirmwareVersion())
                .deviceSecret(deviceSecret)
                .lastSeenAt(device.getLastSeenAt())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    public DeviceDetailResponse toDetailResponse(Device device) {
        Station station = device.getStation();
        Route route = station.getRoute();
        return DeviceDetailResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType())
                .direction(device.getDirection())
                .status(device.getStatus())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .lastSeenAt(device.getLastSeenAt())
                .firmwareVersion(device.getFirmwareVersion())
                .latestIncident(null)
                .build();
    }
}
