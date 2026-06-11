package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    boolean existsByRouteAndStationCode(Route route, String stationCode);

    boolean existsByRouteAndStationOrder(Route route, Integer stationOrder);

    boolean existsByRouteAndStationOrderAndIdNot(Route route, Integer stationOrder, Long id);

    Optional<Station> findByIdAndRouteOperatorId(Long id, Long operatorId);

    @Query("SELECT s FROM Station s " +
            "JOIN s.route r " +
            "WHERE r.operator.id = :operatorId " +
            "AND s.stationCode = :stationCode")
    Optional<Station> findByOperatorIdAndStationCode(@Param("operatorId") Long operatorId,
                                                     @Param("stationCode") String stationCode);

    List<Station> findAllByRouteOrderByStationOrderAsc(Route route);

    @Query("SELECT s.stationCode FROM Station s " +
            "WHERE s.route = :route " +
            "AND s.stationCode LIKE CONCAT(:prefix, '-%')")
    List<String> findStationCodesByRouteAndPrefix(@Param("route") Route route,
                                                  @Param("prefix") String prefix);

    @Query(
            value = "SELECT s FROM Station s " +
                    "JOIN s.route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:routeId IS NULL OR r.id = :routeId) " +
                    "AND (:keywordPattern IS NULL OR LOWER(s.stationCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationName) LIKE :keywordPattern) " +
                    "AND (:status IS NULL OR s.status = :status)",
            countQuery = "SELECT COUNT(s) FROM Station s " +
                    "JOIN s.route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:routeId IS NULL OR r.id = :routeId) " +
                    "AND (:keywordPattern IS NULL OR LOWER(s.stationCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationName) LIKE :keywordPattern) " +
                    "AND (:status IS NULL OR s.status = :status)"
    )
    Page<Station> searchStations(@Param("operatorId") Long operatorId,
                                 @Param("routeId") Long routeId,
                                 @Param("keywordPattern") String keywordPattern,
                                 @Param("status") String status,
                                 Pageable pageable);
}
