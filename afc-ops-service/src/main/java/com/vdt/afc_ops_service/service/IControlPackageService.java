package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.controlpackage.AckControlPackageApplyRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.PublishControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.UpdateControlPackageRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageAckResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackagePublishResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.PendingControlPackageResponse;

import java.util.List;

public interface IControlPackageService {

    ControlPackageResponse create(CreateControlPackageRequest request);

    ControlPackageResponse update(Long packageId, UpdateControlPackageRequest request);

    ControlPackageDetailResponse getDetail(Long packageId);

    PageResponse<ControlPackageResponse> list(String packageType, String sourceType, String status, int page, int size);

    ControlPackagePublishResponse publish(Long packageId, PublishControlPackageRequest request);

    List<PendingControlPackageResponse> pullPending(String stationCode, Long currentVersion);

    ControlPackageAckResponse ackApply(Long syncId, AckControlPackageApplyRequest request);

    PageResponse<ControlPackageSyncResponse> searchSyncs(String packageType, Long version, Long stationId, String status, int page, int size);

    ControlPackageSyncDetailResponse getSyncDetail(Long syncId);
}
