package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.ControlPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ControlPackageRepository extends JpaRepository<ControlPackage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(MAX(cp.version), 0) FROM ControlPackage cp WHERE cp.operator.id = :operatorId")
    Long findMaxVersionForUpdate(@Param("operatorId") Long operatorId);

    Optional<ControlPackage> findByIdAndOperatorId(Long id, Long operatorId);

    @Query(
            value = "SELECT cp FROM ControlPackage cp " +
                    "WHERE cp.operator.id = :operatorId " +
                    "AND (:packageType IS NULL OR cp.packageType = :packageType) " +
                    "AND (:sourceType IS NULL OR cp.sourceType = :sourceType) " +
                    "AND (:status IS NULL OR cp.status = :status)",
            countQuery = "SELECT COUNT(cp) FROM ControlPackage cp " +
                    "WHERE cp.operator.id = :operatorId " +
                    "AND (:packageType IS NULL OR cp.packageType = :packageType) " +
                    "AND (:sourceType IS NULL OR cp.sourceType = :sourceType) " +
                    "AND (:status IS NULL OR cp.status = :status)"
    )
    Page<ControlPackage> searchPackages(@Param("operatorId") Long operatorId,
                                        @Param("packageType") String packageType,
                                        @Param("sourceType") String sourceType,
                                        @Param("status") String status,
                                        Pageable pageable);
}
