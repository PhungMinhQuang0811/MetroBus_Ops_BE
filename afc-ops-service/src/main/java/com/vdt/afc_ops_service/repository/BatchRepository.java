package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Batch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, String> {

    boolean existsByBatchCode(String batchCode);

    @Query("SELECT b.batchCode FROM Batch b " +
            "WHERE b.operator.id = :operatorId " +
            "AND b.batchCode LIKE CONCAT(:prefix, '-%')")
    List<String> findBatchCodesByOperatorAndPrefix(@Param("operatorId") Long operatorId,
                                                   @Param("prefix") String prefix);

    @Query(
            value = "SELECT b FROM Batch b " +
                    "JOIN FETCH b.operator o " +
                    "WHERE o.id = :operatorId " +
                    "AND (:status IS NULL OR b.status = :status) " +
                    "AND b.createdAt >= :fromTime " +
                    "AND b.createdAt <= :toTime",
            countQuery = "SELECT COUNT(b) FROM Batch b " +
                    "WHERE b.operator.id = :operatorId " +
                    "AND (:status IS NULL OR b.status = :status) " +
                    "AND b.createdAt >= :fromTime " +
                    "AND b.createdAt <= :toTime"
    )
    Page<Batch> searchBatches(@Param("operatorId") Long operatorId,
                              @Param("status") String status,
                              @Param("fromTime") LocalDateTime fromTime,
                              @Param("toTime") LocalDateTime toTime,
                              Pageable pageable);
}
