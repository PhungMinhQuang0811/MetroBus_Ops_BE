package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, String> {

    Optional<Entitlement> findByIdAndCardId(String id, String cardId);

    List<Entitlement> findAllByCardIdAndStatusAndValidToAfter(String cardId, String status, LocalDateTime now);
}
