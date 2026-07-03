package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedBatchStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionStatus;
import com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.entity.Batch;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5TransactionService;
import com.vdt.afc_ops_service.mapper.BatchMapper;
import com.vdt.afc_ops_service.repository.BatchRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IBatchService;
import com.vdt.afc_ops_service.service.generator.BatchCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BatchService implements IBatchService {

    static final int MAX_PAGE_SIZE = 100;
    static final LocalDateTime MIN_BATCH_TIME = LocalDateTime.of(1900, 1, 1, 0, 0);
    static final LocalDateTime MAX_BATCH_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    BatchRepository batchRepository;
    TransactionRepository transactionRepository;
    BatchCodeGenerator batchCodeGenerator;
    BatchMapper batchMapper;
    SecurityUtils securityUtils;
    ILevel5TransactionService level5TransactionService;

    @Override
    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        return createBatchForOperator(operator, request);
    }

    @Override
    @Transactional
    public BatchResponse createBatchForOperator(Operator operator, CreateBatchRequest request) {
        LocalDateTime fromTime = request.getFromTime();
        LocalDateTime toTime = request.getToTime();
        if (fromTime.isAfter(toTime)) {
            throw new AppException(ErrorCode.INVALID_BATCH_TIME_RANGE);
        }

        long eligibleCount = transactionRepository.countEligibleForBatch(
                operator.getId(), PredefinedTransactionStatus.PENDING, fromTime, toTime);
        if (eligibleCount == 0) {
            throw new AppException(ErrorCode.BATCH_NO_ELIGIBLE_TRANSACTIONS);
        }

        String batchId = UUID.randomUUID().toString();
        String batchCode = batchCodeGenerator.generate(operator, fromTime.toLocalDate());
        Batch batch = Batch.builder()
                .id(batchId)
                .operator(operator)
                .batchCode(batchCode)
                .fromTime(fromTime)
                .toTime(toTime)
                .transactionCount((int) eligibleCount)
                .status(PredefinedBatchStatus.CREATED)
                .createdByAccountId(SecurityUtils.getCurrentAccountId())
                .build();
        Batch savedBatch = batchRepository.save(batch);

        transactionRepository.assignBatchToEligibleTransactions(
                batchId, operator.getId(), PredefinedTransactionStatus.PENDING, fromTime, toTime);

        return batchMapper.toResponse(savedBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BatchResponse> listBatches(String status, LocalDateTime from, LocalDateTime to,
                                                   int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_BATCH_TIME_RANGE);
        }
        String normalizedStatus = SearchFilterUtil.normalizeUppercase(status);
        LocalDateTime queryFrom = from == null ? MIN_BATCH_TIME : from;
        LocalDateTime queryTo = to == null ? MAX_BATCH_TIME : to;

        Page<Batch> batches = batchRepository.searchBatches(
                operator.getId(),
                normalizedStatus,
                queryFrom,
                queryTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return PageResponse.<BatchResponse>builder()
                .items(batches.getContent().stream()
                        .map(batchMapper::toResponse)
                        .toList())
                .page(batches.getNumber())
                .size(batches.getSize())
                .totalElements(batches.getTotalElements())
                .totalPages(batches.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public BatchResponse submitBatchToLevel5(String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));

        if (!PredefinedBatchStatus.CREATED.equals(batch.getStatus())
                && !PredefinedBatchStatus.FAILED.equals(batch.getStatus())) {
            throw new AppException(ErrorCode.BATCH_NOT_FOUND);
        }

        batch.setStatus(PredefinedBatchStatus.SUBMITTED);
        batch.setSubmittedAt(LocalDateTime.now());
        batchRepository.save(batch);

        try {
            level5TransactionService.publishBatch(batch);
            batch.setStatus(PredefinedBatchStatus.ACCEPTED);
            transactionRepository.updateSyncStatusByBatchId(batchId, "SYNCED");
        } catch (Exception e) {
            batch.setStatus(PredefinedBatchStatus.FAILED);
            batchRepository.save(batch);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        batchRepository.save(batch);
        return batchMapper.toResponse(batch);
    }
}
