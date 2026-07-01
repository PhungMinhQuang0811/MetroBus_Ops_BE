package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.shift.CheckInResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckOutResponse;
import com.vdt.afc_ops_service.dto.response.shift.ShiftResponse;
import com.vdt.afc_ops_service.entity.StationShift;
import org.springframework.stereotype.Component;

@Component
public class ShiftMapper {

    public CheckInResponse toCheckInResponse(StationShift shift) {
        return CheckInResponse.builder()
                .shiftId(shift.getId())
                .accountId(shift.getAccountId())
                .stationId(shift.getStation().getId())
                .stationName(shift.getStation().getStationName())
                .status(shift.getStatus())
                .checkedInAt(shift.getCheckedInAt())
                .build();
    }

    public CheckOutResponse toCheckOutResponse(StationShift shift) {
        return CheckOutResponse.builder()
                .shiftId(shift.getId())
                .status(shift.getStatus())
                .totalTransactions(shift.getTotalTransactions())
                .checkedInAt(shift.getCheckedInAt())
                .checkedOutAt(shift.getCheckedOutAt())
                .build();
    }

    public ShiftResponse toResponse(StationShift shift) {
        return ShiftResponse.builder()
                .id(shift.getId())
                .accountId(shift.getAccountId())
                .stationId(shift.getStation().getId())
                .stationCode(shift.getStation().getStationCode())
                .stationName(shift.getStation().getStationName())
                .routeCode(shift.getStation().getRoute() != null ? shift.getStation().getRoute().getRouteCode() : null)
                .status(shift.getStatus())
                .totalTransactions(shift.getTotalTransactions())
                .checkedInAt(shift.getCheckedInAt())
                .checkedOutAt(shift.getCheckedOutAt())
                .createdAt(shift.getCreatedAt())
                .build();
    }
}