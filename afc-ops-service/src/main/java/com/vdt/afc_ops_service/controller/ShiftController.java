package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.shift.CheckInRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckInResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckOutResponse;
import com.vdt.afc_ops_service.dto.response.shift.ShiftResponse;
import com.vdt.afc_ops_service.service.IShiftService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftController {

    IShiftService shiftService;

    @PostMapping("/check-in")
    public ApiResponse<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.<CheckInResponse>builder()
                .result(shiftService.checkIn(request))
                .build();
    }

    @PostMapping("/check-out")
    public ApiResponse<CheckOutResponse> checkOut() {
        return ApiResponse.<CheckOutResponse>builder()
                .result(shiftService.checkOut())
                .build();
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<ShiftResponse>> listShifts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ShiftResponse>>builder()
                .result(shiftService.listShifts(page, size))
                .build();
    }

    @GetMapping("/list-all")
    public ApiResponse<PageResponse<ShiftResponse>> listAllShifts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ShiftResponse>>builder()
                .result(shiftService.listAllShifts(status, page, size))
                .build();
    }
}
