package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;

public interface IDynamicQrService {

    DynamicQrResponse generate(GenerateDynamicQrRequest request);
}
