package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.service.IBatchService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BatchController {

    IBatchService batchService;

    @PostMapping("/create-batch")
    public ApiResponse<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return ApiResponse.<BatchResponse>builder()
                .result(batchService.createBatch(request))
                .build();
    }

    @PostMapping("/submit-batch-to-level5/{batchId}")
    public ApiResponse<BatchResponse> submitBatchToLevel5(@PathVariable String batchId) {
        return ApiResponse.<BatchResponse>builder()
                .result(batchService.submitBatchToLevel5(batchId))
                .build();
    }

    @GetMapping("/list-batches")
    public ApiResponse<PageResponse<BatchResponse>> listBatches(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<BatchResponse>>builder()
                .result(batchService.listBatches(status, from, to, page, size))
                .build();
    }
}
