package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.request.device.CreateDeviceRequest;
import com.vdt.afc_ops_service.dto.request.device.UpdateDeviceRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceProvisionResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceConfirmResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewResponse;
import com.vdt.afc_ops_service.service.IDeviceService;
import com.vdt.afc_ops_service.service.Impl.DeviceImportService;
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
@RequestMapping("/device")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceController {

    IDeviceService deviceService;
    DeviceImportService deviceImportService;

    @GetMapping("/list-devices")
    public ApiResponse<PageResponse<DeviceResponse>> listDevices(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<DeviceResponse>>builder()
                .result(deviceService.listDevices(stationId, deviceType, status, keyword, page, size))
                .build();
    }

    @GetMapping("/get-device/{deviceId}")
    public ApiResponse<DeviceDetailResponse> getDevice(@PathVariable Long deviceId) {
        return ApiResponse.<DeviceDetailResponse>builder()
                .result(deviceService.getDevice(deviceId))
                .build();
    }

    @PostMapping("/create-device")
    public ApiResponse<DeviceProvisionResponse> createDevice(@Valid @RequestBody CreateDeviceRequest request) {
        return ApiResponse.<DeviceProvisionResponse>builder()
                .result(deviceService.createDevice(request))
                .build();
    }

    @PostMapping("/update-device/{deviceId}")
    public ApiResponse<DeviceResponse> updateDevice(
            @PathVariable Long deviceId,
            @Valid @RequestBody UpdateDeviceRequest request
    ) {
        return ApiResponse.<DeviceResponse>builder()
                .result(deviceService.updateDevice(deviceId, request))
                .build();
    }

    @PostMapping("/enable-device/{deviceId}")
    public ApiResponse<DeviceResponse> enableDevice(@PathVariable Long deviceId) {
        return ApiResponse.<DeviceResponse>builder()
                .result(deviceService.enableDevice(deviceId))
                .build();
    }

    @PostMapping("/disable-device/{deviceId}")
    public ApiResponse<DeviceResponse> disableDevice(@PathVariable Long deviceId) {
        return ApiResponse.<DeviceResponse>builder()
                .result(deviceService.disableDevice(deviceId))
                .build();
    }

    @PostMapping("/preview-import-devices")
    public ApiResponse<ImportDevicePreviewResponse> previewImportDevices(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportDevicePreviewResponse>builder()
                .result(deviceImportService.preview(resolveSingleImportFile(files)))
                .build();
    }

    @PostMapping("/confirm-import-devices")
    public ApiResponse<ImportDeviceConfirmResponse> confirmImportDevices(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportDeviceConfirmResponse>builder()
                .result(deviceImportService.confirm(resolveSingleImportFile(files)))
                .build();
    }

    private MultipartFile resolveSingleImportFile(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        return files.get(0);
    }
}
