package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query(
            value = "SELECT t FROM Transaction t " +
                    "JOIN FETCH t.operator o " +
                    "JOIN FETCH t.route r " +
                    "JOIN FETCH t.station s " +
                    "JOIN FETCH t.device d " +
                    "LEFT JOIN FETCH t.card c " +
                    "LEFT JOIN FETCH t.ticket tk " +
                    "LEFT JOIN FETCH t.entitlement e " +
                    "WHERE o.id = :operatorId " +
                    "AND t.occurredAt >= :fromTime " +
                    "AND t.occurredAt <= :toTime " +
                    "AND (:routeId IS NULL OR r.id = :routeId) " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:deviceId IS NULL OR d.id = :deviceId) " +
                    "AND (:cardId IS NULL OR c.id = :cardId) " +
                    "AND (:ticketId IS NULL OR tk.id = :ticketId) " +
                    "AND (:entitlementId IS NULL OR e.id = :entitlementId) " +
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
                    "LEFT JOIN t.entitlement e " +
                    "WHERE o.id = :operatorId " +
                    "AND t.occurredAt >= :fromTime " +
                    "AND t.occurredAt <= :toTime " +
                    "AND (:hasRouteId = false OR r.id = :routeId) " +
                    "AND (:hasStationId = false OR s.id = :stationId) " +
                    "AND (:hasDeviceId = false OR d.id = :deviceId) " +
                    "AND (:hasCardId = false OR c.id = :cardId) " +
                    "AND (:hasTicketId = false OR tk.id = :ticketId) " +
                    "AND (:hasEntitlementId = false OR e.id = :entitlementId) " +
                    "AND (:hasTapType = false OR t.tapType = :tapType) " +
                    "AND (:hasDecision = false OR t.decision = :decision) " +
                    "AND (:hasReason = false OR t.reason = :reason) " +
                    "AND (:hasSyncStatus = false OR t.syncStatus = :syncStatus) " +
                    "AND (:hasTicketProcessingStatus = false OR t.ticketProcessingStatus = :ticketProcessingStatus)"
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
            "LEFT JOIN FETCH t.entitlement e " +
            "WHERE t.id = :transactionId AND o.id = :operatorId")
    Optional<Transaction> findDetailByIdAndOperatorId(@Param("transactionId") String transactionId,
                                                      @Param("operatorId") Long operatorId);

    boolean existsById(String transactionId);
}
