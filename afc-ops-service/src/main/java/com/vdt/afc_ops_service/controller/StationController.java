package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.request.station.CreateStationRequest;
import com.vdt.afc_ops_service.dto.request.station.UpdateStationRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationConfirmResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDetailResponse;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.service.IStationService;
import com.vdt.afc_ops_service.service.Impl.StationImportService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StationController {

    IStationService stationService;
    StationImportService stationImportService;

    @GetMapping("/list-stations")
    public ApiResponse<PageResponse<StationResponse>> listStations(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<StationResponse>>builder()
                .result(stationService.listStations(routeId, keyword, status, page, size))
                .build();
    }

    @GetMapping("/get-station/{stationId}")
    public ApiResponse<StationDetailResponse> getStation(@PathVariable Long stationId) {
        return ApiResponse.<StationDetailResponse>builder()
                .result(stationService.getStation(stationId))
                .build();
    }

    @PostMapping("/create-station")
    public ApiResponse<StationResponse> createStation(@Valid @RequestBody CreateStationRequest request) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.createStation(request))
                .build();
    }

    @PostMapping("/update-station/{stationId}")
    public ApiResponse<StationResponse> updateStation(
            @PathVariable Long stationId,
            @Valid @RequestBody UpdateStationRequest request
    ) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.updateStation(stationId, request))
                .build();
    }

    @PostMapping("/enable-station/{stationId}")
    public ApiResponse<StationResponse> enableStation(@PathVariable Long stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.enableStation(stationId))
                .build();
    }

    @PostMapping("/disable-station/{stationId}")
    public ApiResponse<StationResponse> disableStation(@PathVariable Long stationId) {
        return ApiResponse.<StationResponse>builder()
                .result(stationService.disableStation(stationId))
                .build();
    }

    @PostMapping("/preview-import-stations")
    public ApiResponse<ImportStationPreviewResponse> previewImportStations(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportStationPreviewResponse>builder()
                .result(stationImportService.preview(resolveSingleImportFile(files)))
                .build();
    }

    @PostMapping("/confirm-import-stations")
    public ApiResponse<ImportStationConfirmResponse> confirmImportStations(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportStationConfirmResponse>builder()
                .result(stationImportService.confirm(resolveSingleImportFile(files)))
                .build();
    }

    private MultipartFile resolveSingleImportFile(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        return files.get(0);
    }
}
