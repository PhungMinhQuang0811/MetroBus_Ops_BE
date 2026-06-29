package com.vdt.afc_ops_service.integration.level5.scheduler;

import com.vdt.afc_ops_service.integration.level5.service.ILevel5TransactionService;
import com.vdt.afc_ops_service.mapper.BatchMapper;
import com.vdt.afc_ops_service.repository.BatchRepository;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.service.IBatchService;
import com.vdt.afc_ops_service.constant.PredefinedBatchStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionStatus;
import com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest;
import com.vdt.afc_ops_service.entity.Batch;
import com.vdt.afc_ops_service.entity.Operator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BatchSubmitScheduler {

    TransactionRepository transactionRepository;
    OperatorRepository operatorRepository;
    BatchRepository batchRepository;
    IBatchService batchService;

    @Scheduled(cron = "0 0 1 * * ?") // 1:00 AM every day
    public void autoCreateAndSubmitBatches() {
        log.info("===== Auto batch submit started =====");

        List<Operator> operators = operatorRepository.findAll();
        if (operators.isEmpty()) {
            log.info("No operators found. Skipping auto batch submit.");
            return;
        }

        for (Operator operator : operators) {
            try {
                processOperatorBatches(operator);
            } catch (Exception e) {
                log.error("Failed to process auto batch for operator {}: {}",
                        operator.getId(), e.getMessage(), e);
            }
        }

        log.info("===== Auto batch submit completed =====");
    }

    private void processOperatorBatches(Operator operator) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime fromTime = yesterday.atStartOfDay();
        LocalDateTime toTime = yesterday.atTime(LocalTime.MAX);

        long pendingCount = transactionRepository.countEligibleForBatch(
                operator.getId(), PredefinedTransactionStatus.PENDING, fromTime, toTime);

        if (pendingCount == 0) {
            log.debug("No PENDING transactions for operator {} on {}. Skipping.", operator.getId(), yesterday);
            return;
        }

        log.info("Found {} PENDING transactions for operator {} on {}. Creating batch...",
                pendingCount, operator.getId(), yesterday);

        try {
            // Bước 1: Tạo batch
            CreateBatchRequest request = new CreateBatchRequest(fromTime, toTime);
            var batch = batchService.createBatch(request);
            String batchId = batch.getId();

            log.info("Auto batch created: operator={}, batchId={}, transactions={}",
                    operator.getId(), batchId, pendingCount);

            // Bước 2: Gửi batch lên C5
            batchService.submitBatchToLevel5(batchId);

            log.info("Auto batch submitted successfully: operator={}, batchId={}",
                    operator.getId(), batchId);
        } catch (Exception e) {
            log.error("Failed to auto-create/submit batch for operator {}: {}",
                    operator.getId(), e.getMessage());
        }
    }
}