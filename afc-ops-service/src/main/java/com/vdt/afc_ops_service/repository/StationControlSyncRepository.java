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
import java.time.LocalDateTime;

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

    @Query(value = """
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN scs.sync_status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending,
                COALESCE(SUM(CASE WHEN scs.sync_status = 'APPLIED' THEN 1 ELSE 0 END), 0) AS applied,
                COALESCE(SUM(CASE WHEN scs.sync_status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed
            FROM station_control_syncs scs
            JOIN control_packages cp ON scs.control_package_id = cp.id
            JOIN stations s ON scs.station_id = s.id
            JOIN routes r ON s.route_id = r.id
            WHERE cp.operator_id = :operatorId
              AND scs.created_at >= :fromTime
              AND scs.created_at <= :toTime
              AND (:routeId IS NULL OR r.id = :routeId)
              AND (:stationId IS NULL OR s.id = :stationId)
            """, nativeQuery = true)
    List<Object[]> getDashboardControlSyncSummary(@Param("operatorId") Long operatorId,
                                                  @Param("fromTime") LocalDateTime fromTime,
                                                  @Param("toTime") LocalDateTime toTime,
                                                  @Param("routeId") Long routeId,
                                                  @Param("stationId") Long stationId);
}
