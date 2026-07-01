package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.request.shift.CheckInRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckInResponse;
import com.vdt.afc_ops_service.dto.response.shift.CheckOutResponse;
import com.vdt.afc_ops_service.dto.response.shift.ShiftResponse;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationShift;
import com.vdt.afc_ops_service.mapper.ShiftMapper;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.StationShiftRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IShiftService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftService implements IShiftService {

    static final int MAX_PAGE_SIZE = 100;

    StationShiftRepository shiftRepository;
    StationRepository stationRepository;
    TransactionRepository transactionRepository;
    ShiftMapper shiftMapper;
    SecurityUtils securityUtils;

    @Override
    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {
        String accountId = securityUtils.getCurrentAccountId();

        // 1. Validate station tồn tại và ACTIVE
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // 2. Kiểm tra account chưa có shift CHECKED_IN
        shiftRepository.findByAccountIdAndStatus(accountId, "CHECKED_IN")
                .ifPresent(shift -> {
                    throw new AppException(ErrorCode.SHIFT_ALREADY_CHECKED_IN);
                });

        // 3. Tạo shift mới
        StationShift shift = StationShift.builder()
                .accountId(accountId)
                .station(station)
                .status("CHECKED_IN")
                .totalTransactions(0)
                .checkedInAt(LocalDateTime.now())
                .build();

        shift = shiftRepository.save(shift);
        return shiftMapper.toCheckInResponse(shift);
    }

    @Override
    @Transactional
    public CheckOutResponse checkOut() {
        String accountId = securityUtils.getCurrentAccountId();

        // 1. Tìm shift CHECKED_IN của account
        StationShift shift = shiftRepository.findByAccountIdAndStatus(accountId, "CHECKED_IN")
                .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SHIFT));

        // 2. Đếm transaction trong ca
        int totalTx = countTransactionsInShift(shift);

        // 3. Update shift
        shift.setStatus("CHECKED_OUT");
        shift.setTotalTransactions(totalTx);
        shift.setCheckedOutAt(LocalDateTime.now());
        shift = shiftRepository.save(shift);

        return shiftMapper.toCheckOutResponse(shift);
    }

    @Override
    public PageResponse<ShiftResponse> listShifts(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        String accountId = securityUtils.getCurrentAccountId();
        Page<StationShift> shiftPage = shiftRepository.findRecentByAccountId(
                accountId, PageRequest.of(page, size));

        List<ShiftResponse> items = shiftPage.getContent().stream()
                .map(shiftMapper::toResponse)
                .toList();

        return PageResponse.<ShiftResponse>builder()
                .items(items)
                .page(shiftPage.getNumber())
                .size(shiftPage.getSize())
                .totalElements(shiftPage.getTotalElements())
                .totalPages(shiftPage.getTotalPages())
                .build();
    }

    @Override
    public PageResponse<ShiftResponse> listAllShifts(String status, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        Long operatorId = securityUtils.getRequiredCurrentOperator().getId();
        Page<StationShift> shiftPage = shiftRepository.findAllByOperatorId(
                operatorId, status, PageRequest.of(page, size));

        List<ShiftResponse> items = shiftPage.getContent().stream()
                .map(shiftMapper::toResponse)
                .toList();

        return PageResponse.<ShiftResponse>builder()
                .items(items)
                .page(shiftPage.getNumber())
                .size(shiftPage.getSize())
                .totalElements(shiftPage.getTotalElements())
                .totalPages(shiftPage.getTotalPages())
                .build();
    }

    private int countTransactionsInShift(StationShift shift) {
        Long stationId = shift.getStation().getId();
        LocalDateTime from = shift.getCheckedInAt();
        LocalDateTime to = LocalDateTime.now();

        return (int) transactionRepository.countEligibleForBatch(
                shift.getStation().getRoute().getOperator().getId(),
                "PENDING", from, to);
    }
}