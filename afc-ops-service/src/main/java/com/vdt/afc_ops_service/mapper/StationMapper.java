package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.station.StationDetailResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDeviceResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDeviceSummary;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StationMapper {

    public StationResponse toStationResponse(Station station) {
        Route route = station.getRoute();
        return StationResponse.builder()
                .id(station.getId())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .stationOrder(station.getStationOrder())
                .status(station.getStatus())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }

    public StationDetailResponse toStationDetailResponse(Station station, List<Device> devices) {
        Route route = station.getRoute();
        return StationDetailResponse.builder()
                .id(station.getId())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .stationOrder(station.getStationOrder())
                .status(station.getStatus())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .deviceSummary(toDeviceSummary(devices))
                .devices(devices.stream().map(this::toDeviceResponse).toList())
                .build();
    }

    public StationDeviceResponse toDeviceResponse(Device device) {
        return StationDeviceResponse.builder()
                .id(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType())
                .direction(device.getDirection())
                .status(device.getStatus())
                .firmwareVersion(device.getFirmwareVersion())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }

    public StationDeviceSummary toDeviceSummary(List<Device> devices) {
        return StationDeviceSummary.builder()
                .total(devices.size())
                .active(countDevicesByStatus(devices, "ACTIVE"))
                .offline(countDevicesByStatus(devices, "OFFLINE"))
                .maintenance(countDevicesByStatus(devices, "MAINTENANCE"))
                .disabled(countDevicesByStatus(devices, "DISABLED"))
                .build();
    }

    private int countDevicesByStatus(List<Device> devices, String status) {
        return (int) devices.stream().filter(device -> status.equals(device.getStatus())).count();
    }
}
