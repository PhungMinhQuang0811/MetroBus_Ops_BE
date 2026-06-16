package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedBatchStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionStatus;
import com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.entity.Batch;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.mapper.BatchMapper;
import com.vdt.afc_ops_service.repository.BatchRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.BatchService;
import com.vdt.afc_ops_service.service.generator.BatchCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    BatchRepository batchRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    BatchCodeGenerator batchCodeGenerator;

    @Mock
    SecurityUtils securityUtils;

    BatchService service;

    @BeforeEach
    void setUp() {
        service = new BatchService(batchRepository, transactionRepository, batchCodeGenerator,
                new BatchMapper(), securityUtils);
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator());
    }

    @Test
    void createBatch_WithEligibleTransactions_CreatesBatchAndAssignsTransactions() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 4, 23, 59, 59);
        when(transactionRepository.countEligibleForBatch(eq(1L), eq(PredefinedTransactionStatus.PENDING),
                eq(from), eq(to))).thenReturn(1500L);
        when(batchCodeGenerator.generate(any(), eq(from.toLocalDate()))).thenReturn("HCMC-METRO-20260604-0001");
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BatchResponse response = service.createBatch(CreateBatchRequest.builder()
                .fromTime(from).toTime(to).build());

        assertEquals("HCMC-METRO-20260604-0001", response.getBatchCode());
        assertEquals(1500, response.getTransactionCount());
        assertEquals(PredefinedBatchStatus.CREATED, response.getStatus());

        ArgumentCaptor<Batch> batchCaptor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(batchCaptor.capture());
        verify(transactionRepository).assignBatchToEligibleTransactions(
                eq(batchCaptor.getValue().getId()), eq(1L), eq(PredefinedTransactionStatus.PENDING),
                eq(from), eq(to));
    }

    @Test
    void createBatch_NoEligibleTransactions_Throws() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 4, 23, 59, 59);
        when(transactionRepository.countEligibleForBatch(eq(1L), eq(PredefinedTransactionStatus.PENDING),
                eq(from), eq(to))).thenReturn(0L);

        AppException exception = assertThrows(AppException.class, () -> service.createBatch(
                CreateBatchRequest.builder().fromTime(from).toTime(to).build()));

        assertEquals(ErrorCode.BATCH_NO_ELIGIBLE_TRANSACTIONS, exception.getErrorCode());
        verify(batchRepository, never()).save(any());
        verify(transactionRepository, never()).assignBatchToEligibleTransactions(any(), any(), any(), any(), any());
    }

    @Test
    void createBatch_FromAfterTo_Throws() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 4, 0, 0);

        AppException exception = assertThrows(AppException.class, () -> service.createBatch(
                CreateBatchRequest.builder().fromTime(from).toTime(to).build()));

        assertEquals(ErrorCode.INVALID_BATCH_TIME_RANGE, exception.getErrorCode());
        verify(transactionRepository, never()).countEligibleForBatch(any(), any(), any(), any());
    }

    @Test
    void listBatches_ReturnsPagedResults() {
        when(batchRepository.searchBatches(eq(1L), eq("CREATED"), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(batch())));

        PageResponse<BatchResponse> response = service.listBatches("created", null, null, 0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals("HCMC-METRO-20260604-0001", response.getItems().get(0).getBatchCode());
    }

    @Test
    void listBatches_InvalidPage_Throws() {
        AppException exception = assertThrows(AppException.class,
                () -> service.listBatches(null, null, null, -1, 20));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listBatches_SizeTooSmall_Throws() {
        AppException exception = assertThrows(AppException.class,
                () -> service.listBatches(null, null, null, 0, 0));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listBatches_SizeTooLarge_Throws() {
        AppException exception = assertThrows(AppException.class,
                () -> service.listBatches(null, null, null, 0, 101));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listBatches_FromAfterTo_Throws() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 4, 0, 0);

        AppException exception = assertThrows(AppException.class,
                () -> service.listBatches(null, from, to, 0, 20));
        assertEquals(ErrorCode.INVALID_BATCH_TIME_RANGE, exception.getErrorCode());
    }

    @Test
    void listBatches_WithDateRange_UsesProvidedBounds() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 4, 23, 59, 59);
        when(batchRepository.searchBatches(eq(1L), eq(null), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(batch())));

        PageResponse<BatchResponse> response = service.listBatches(null, from, to, 0, 20);

        assertEquals(1, response.getTotalElements());
        verify(batchRepository).searchBatches(eq(1L), eq(null), eq(from), eq(to), any(Pageable.class));
    }

    private Operator operator() {
        return Operator.builder().id(1L).operatorCode("HCMC-METRO").operatorName("HCMC Metro").build();
    }

    private Batch batch() {
        return Batch.builder()
                .id("BATCH-000001")
                .operator(operator())
                .batchCode("HCMC-METRO-20260604-0001")
                .fromTime(LocalDateTime.of(2026, 6, 4, 0, 0))
                .toTime(LocalDateTime.of(2026, 6, 4, 23, 59, 59))
                .transactionCount(1500)
                .status(PredefinedBatchStatus.CREATED)
                .build();
    }
}
