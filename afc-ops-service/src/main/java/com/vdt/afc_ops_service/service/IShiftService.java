package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.shift.CheckInRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckInResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckOutResponse;
import com.vdt.afc_ops_service.dto.response.shift.ShiftResponse;

public interface IShiftService {

    CheckInResponse checkIn(CheckInRequest request);

    CheckOutResponse checkOut();

    PageResponse<ShiftResponse> listShifts(int page, int size);
}