package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;
import com.vdt.afc_ops_service.service.IDynamicQrService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamicQrController {

    IDynamicQrService dynamicQrService;

    @PostMapping("/generate-dynamic-qr")
    public ApiResponse<DynamicQrResponse> generateDynamicQr(
            @RequestHeader(value = "X-External-User-Id", required = false) String externalUserId,
            @Valid @RequestBody GenerateDynamicQrRequest request
    ) {
        return ApiResponse.<DynamicQrResponse>builder()
                .result(dynamicQrService.generate(request))
                .build();
    }
}
