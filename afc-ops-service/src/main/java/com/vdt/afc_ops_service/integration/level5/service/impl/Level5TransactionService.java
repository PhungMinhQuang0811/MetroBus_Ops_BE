package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.entity.Batch;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5TransactionService;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class Level5TransactionService implements ILevel5TransactionService {

    static final String TRANSACTION_BATCH_KEY = "transaction.batch";

    RabbitTemplate rabbitTemplate;
    TopicExchange afcExchange;
    TransactionRepository transactionRepository;

    @Override
    public void publishBatch(Batch batch) {
        List<Transaction> transactions = transactionRepository.findAllByBatchId(batch.getId());

        if (transactions.isEmpty()) {
            log.warn("No transactions found for batch {}", batch.getBatchCode());
            return;
        }

        List<Map<String, Object>> transactionItems = transactions.stream()
                .map(tx -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("transactionId", tx.getId());
                    item.put("cardUid", tx.getCard() != null && tx.getCard().getCardUid() != null
                            ? tx.getCard().getCardUid() : "");
                    item.put("ticketId", tx.getTicket() != null ? tx.getTicket().getId() : null);
                    item.put("operatorCode", tx.getOperator() != null ? tx.getOperator().getOperatorCode() : "");
                    item.put("lineCode", tx.getRoute() != null ? tx.getRoute().getRouteCode() : "");
                    item.put("tapInStationCode", tx.getTapType().equals("TAP_IN") && tx.getStation() != null
                            ? tx.getStation().getStationCode() : null);
                    item.put("tapInAt", tx.getTapType().equals("TAP_IN")
                            ? tx.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toString()
                            : null);
                    item.put("tapOutStationCode", tx.getTapType().equals("TAP_OUT") && tx.getStation() != null
                            ? tx.getStation().getStationCode() : null);
                    item.put("tapOutAt", tx.getTapType().equals("TAP_OUT")
                            ? tx.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toString()
                            : null);
                    item.put("mode", tx.getRoute() != null ? tx.getRoute().getTransportType() : "METRO");
                    item.put("ticketType", tx.getTicket() != null ? tx.getTicket().getType() : null);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> batchPayload = Map.of(
                "transactions", transactionItems
        );

        try {
            rabbitTemplate.convertAndSend(afcExchange.getName(), TRANSACTION_BATCH_KEY, batchPayload);
            log.info("Published batch {} with {} transactions to Level 5 via routing key {}",
                    batch.getBatchCode(), transactions.size(), TRANSACTION_BATCH_KEY);
        } catch (Exception e) {
            log.error("Failed to publish batch {} to Level 5", batch.getBatchCode(), e);
            throw new RuntimeException("Failed to publish batch to Level 5", e);
        }
    }
}