package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.StationControlSync;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationControlSyncRepository extends JpaRepository<StationControlSync, Long> {

    boolean existsByControlPackageId(Long controlPackageId);

    Optional<StationControlSync> findByStationIdAndControlPackageId(Long stationId, Long controlPackageId);

    @EntityGraph(attributePaths = {"station", "station.route", "controlPackage"})
    List<StationControlSync> findAllByStationStationCodeAndSyncStatusAndControlPackageVersionGreaterThanOrderByControlPackageVersionAsc(
            String stationCode,
            String syncStatus,
            Long currentVersion
    );

    @EntityGraph(attributePaths = {"station", "station.route", "controlPackage"})
    Optional<StationControlSync> findWithRelationsById(Long id);

    @Query(
            value = "SELECT scs FROM StationControlSync scs " +
                    "JOIN scs.station s " +
                    "JOIN s.route r " +
                    "JOIN scs.controlPackage cp " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:packageType IS NULL OR cp.packageType = :packageType) " +
                    "AND (:version IS NULL OR cp.version = :version) " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:status IS NULL OR scs.syncStatus = :status)",
            countQuery = "SELECT COUNT(scs) FROM StationControlSync scs " +
                    "JOIN scs.station s " +
                    "JOIN s.route r " +
                    "JOIN scs.controlPackage cp " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:packageType IS NULL OR cp.packageType = :packageType) " +
                    "AND (:version IS NULL OR cp.version = :version) " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:status IS NULL OR scs.syncStatus = :status)"
    )
    Page<StationControlSync> searchSyncs(@Param("operatorId") Long operatorId,
                                         @Param("packageType") String packageType,
                                         @Param("version") Long version,
                                         @Param("stationId") Long stationId,
                                         @Param("status") String status,
                                         Pageable pageable);

    @Query("SELECT scs FROM StationControlSync scs " +
            "JOIN FETCH scs.station s " +
            "JOIN FETCH s.route r " +
            "JOIN FETCH scs.controlPackage cp " +
            "WHERE scs.id = :syncId AND r.operator.id = :operatorId")
    Optional<StationControlSync> findDetailByIdAndOperatorId(@Param("syncId") Long syncId,
                                                             @Param("operatorId") Long operatorId);
}
