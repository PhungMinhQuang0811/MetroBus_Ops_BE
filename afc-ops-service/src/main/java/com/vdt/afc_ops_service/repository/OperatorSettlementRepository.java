package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.OperatorSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorSettlementRepository extends JpaRepository<OperatorSettlement, Long> {

    Optional<OperatorSettlement> findBySettlementIdAndOperatorCode(String settlementId, String operatorCode);

    Page<OperatorSettlement> findByOperatorCodeOrderByCreatedAtDesc(String operatorCode, Pageable pageable);
}