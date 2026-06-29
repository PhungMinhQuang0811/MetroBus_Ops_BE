package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query(
            value = "SELECT t FROM Transaction t " +
                    "JOIN FETCH t.operator o " +
                    "JOIN FETCH t.route r " +
                    "JOIN FETCH t.station s " +
                    "JOIN FETCH t.device d " +
                    "LEFT JOIN FETCH t.card c " +
                    "LEFT JOIN FETCH t.ticket tk " +
                    "WHERE o.id = :operatorId " +
                    "AND t.occurredAt >= :fromTime " +
                    "AND t.occurredAt <= :toTime " +
                    "AND (:routeId IS NULL OR r.id = :routeId) " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:deviceId IS NULL OR d.id = :deviceId) " +
                    "AND (:cardId IS NULL OR c.id = :cardId) " +
                    "AND (:ticketId IS NULL OR tk.id = :ticketId) " +
                    "AND (:tapType IS NULL OR t.tapType = :tapType) " +
                    "AND (:decision IS NULL OR t.decision = :decision) " +
                    "AND (:reason IS NULL OR t.reason = :reason) " +
                    "AND (:syncStatus IS NULL OR t.syncStatus = :syncStatus) " +
                    "AND (:ticketProcessingStatus IS NULL OR t.ticketProcessingStatus = :ticketProcessingStatus)",
            countQuery = "SELECT COUNT(t) FROM Transaction t " +
                    "JOIN t.operator o " +
                    "JOIN t.route r " +
                    "JOIN t.station s " +
                    "JOIN t.device d " +
                    "LEFT JOIN t.card c " +
                    "LEFT JOIN t.ticket tk " +
                    "WHERE o.id = :operatorId " +
                    "AND t.occurredAt >= :fromTime " +
                    "AND t.occurredAt <= :toTime " +
                    "AND (:routeId IS NULL OR r.id = :routeId) " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:deviceId IS NULL OR d.id = :deviceId) " +
                    "AND (:cardId IS NULL OR c.id = :cardId) " +
                    "AND (:ticketId IS NULL OR tk.id = :ticketId) " +
                    "AND (:tapType IS NULL OR t.tapType = :tapType) " +
                    "AND (:decision IS NULL OR t.decision = :decision) " +
                    "AND (:reason IS NULL OR t.reason = :reason) " +
                    "AND (:syncStatus IS NULL OR t.syncStatus = :syncStatus) " +
                    "AND (:ticketProcessingStatus IS NULL OR t.ticketProcessingStatus = :ticketProcessingStatus)"
    )
    Page<Transaction> searchTransactions(@Param("operatorId") Long operatorId,
                                         @Param("fromTime") LocalDateTime from,
                                         @Param("toTime") LocalDateTime to,
                                         @Param("routeId") Long routeId,
                                         @Param("stationId") Long stationId,
                                         @Param("deviceId") Long deviceId,
                                         @Param("cardId") String cardId,
                                         @Param("ticketId") String ticketId,
                                         @Param("entitlementId") String entitlementId,
                                         @Param("tapType") String tapType,
                                         @Param("decision") String decision,
                                         @Param("reason") String reason,
                                         @Param("syncStatus") String syncStatus,
                                         @Param("ticketProcessingStatus") String ticketProcessingStatus,
                                         Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.operator o " +
            "JOIN FETCH t.route r " +
            "JOIN FETCH t.station s " +
            "JOIN FETCH t.device d " +
            "LEFT JOIN FETCH t.card c " +
            "LEFT JOIN FETCH t.ticket tk " +
            "WHERE t.id = :transactionId AND o.id = :operatorId")
    Optional<Transaction> findDetailByIdAndOperatorId(@Param("transactionId") String transactionId,
                                                      @Param("operatorId") Long operatorId);

    boolean existsById(String transactionId);

    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE t.operator.id = :operatorId " +
            "AND t.syncStatus = :syncStatus " +
            "AND t.batchId IS NULL " +
            "AND t.occurredAt >= :fromTime " +
            "AND t.occurredAt <= :toTime")
    long countEligibleForBatch(@Param("operatorId") Long operatorId,
                               @Param("syncStatus") String syncStatus,
                               @Param("fromTime") LocalDateTime fromTime,
                               @Param("toTime") LocalDateTime toTime);

    @Modifying
    @Query("UPDATE Transaction t SET t.batchId = :batchId " +
            "WHERE t.operator.id = :operatorId " +
            "AND t.syncStatus = :syncStatus " +
            "AND t.batchId IS NULL " +
            "AND t.occurredAt >= :fromTime " +
            "AND t.occurredAt <= :toTime")
    int assignBatchToEligibleTransactions(@Param("batchId") String batchId,
                                          @Param("operatorId") Long operatorId,
                                          @Param("syncStatus") String syncStatus,
                                          @Param("fromTime") LocalDateTime fromTime,
                                          @Param("toTime") LocalDateTime toTime);

    @Query(value = """
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN t.decision = 'OPEN_GATE' THEN 1 ELSE 0 END), 0) AS open_gate,
                COALESCE(SUM(CASE WHEN t.decision = 'DENY' THEN 1 ELSE 0 END), 0) AS deny,
                COALESCE(SUM(CASE WHEN t.decision = 'ACCEPTED_FOR_FORWARDING' THEN 1 ELSE 0 END), 0) AS accepted_forwarding
            FROM transactions t
            WHERE t.operator_id = :operatorId
              AND t.occurred_at >= :fromTime
              AND t.occurred_at <= :toTime
              AND (:routeId IS NULL OR t.route_id = :routeId)
              AND (:stationId IS NULL OR t.station_id = :stationId)
            """, nativeQuery = true)
    List<Object[]> getDashboardTransactionSummary(@Param("operatorId") Long operatorId,
                                                  @Param("fromTime") LocalDateTime fromTime,
                                                  @Param("toTime") LocalDateTime toTime,
                                                  @Param("routeId") Long routeId,
                                                  @Param("stationId") Long stationId);

    @Query(value = """
            SELECT
                date_trunc(cast(:bucket as text), t.occurred_at) AS bucket_time,
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN t.decision = 'OPEN_GATE' THEN 1 ELSE 0 END), 0) AS open_gate,
                COALESCE(SUM(CASE WHEN t.decision = 'DENY' THEN 1 ELSE 0 END), 0) AS deny,
                COALESCE(SUM(CASE WHEN t.decision = 'ACCEPTED_FOR_FORWARDING' THEN 1 ELSE 0 END), 0) AS accepted_forwarding
            FROM transactions t
            WHERE t.operator_id = :operatorId
              AND t.occurred_at >= :fromTime
              AND t.occurred_at <= :toTime
              AND (:routeId IS NULL OR t.route_id = :routeId)
              AND (:stationId IS NULL OR t.station_id = :stationId)
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> getDashboardTransactionTimeline(@Param("operatorId") Long operatorId,
                                                   @Param("fromTime") LocalDateTime fromTime,
                                                   @Param("toTime") LocalDateTime toTime,
                                                   @Param("routeId") Long routeId,
                                                   @Param("stationId") Long stationId,
                                                   @Param("bucket") String bucket);

    @Query(value = """
            SELECT
                r.id AS route_id,
                r.route_code AS route_code,
                r.route_name AS route_name,
                s.id AS station_id,
                s.station_code AS station_code,
                s.station_name AS station_name,
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN t.decision = 'OPEN_GATE' THEN 1 ELSE 0 END), 0) AS open_gate,
                COALESCE(SUM(CASE WHEN t.decision = 'DENY' THEN 1 ELSE 0 END), 0) AS deny
            FROM transactions t
            JOIN routes r ON t.route_id = r.id
            JOIN stations s ON t.station_id = s.id
            WHERE t.operator_id = :operatorId
              AND t.occurred_at >= :fromTime
              AND t.occurred_at <= :toTime
              AND (:routeId IS NULL OR r.id = :routeId)
              AND (:stationId IS NULL OR s.id = :stationId)
            GROUP BY r.id, r.route_code, r.route_name, s.id, s.station_code, s.station_name, s.station_order
            ORDER BY r.route_code, s.station_order
            """, nativeQuery = true)
    List<Object[]> getDashboardRouteStationSummaries(@Param("operatorId") Long operatorId,
                                                      @Param("fromTime") LocalDateTime fromTime,
                                                      @Param("toTime") LocalDateTime toTime,
                                                      @Param("routeId") Long routeId,
                                                      @Param("stationId") Long stationId);

    @Query("SELECT t FROM Transaction t WHERE t.batchId = :batchId")
    List<Transaction> findAllByBatchId(@Param("batchId") String batchId);

    @Modifying
    @Query("UPDATE Transaction t SET t.syncStatus = :syncStatus WHERE t.batchId = :batchId")
    int updateSyncStatusByBatchId(@Param("batchId") String batchId, @Param("syncStatus") String syncStatus);
}
