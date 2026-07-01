package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.StationShift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StationShiftRepository extends JpaRepository<StationShift, Long> {

    Optional<StationShift> findByAccountIdAndStatus(String accountId, String status);

    @Query("SELECT ss FROM StationShift ss " +
            "JOIN FETCH ss.station s " +
            "JOIN FETCH s.route r " +
            "WHERE ss.accountId = :accountId " +
            "ORDER BY ss.checkedInAt DESC")
    Page<StationShift> findRecentByAccountId(@Param("accountId") String accountId, Pageable pageable);

    @Query("SELECT ss FROM StationShift ss " +
            "JOIN FETCH ss.station s " +
            "JOIN FETCH s.route r " +
            "JOIN r.operator o " +
            "WHERE o.id = :operatorId " +
            "AND (:status IS NULL OR ss.status = :status) " +
            "ORDER BY ss.checkedInAt DESC")
    Page<StationShift> findAllByOperatorId(@Param("operatorId") Long operatorId,
                                           @Param("status") String status,
                                           Pageable pageable);
}
