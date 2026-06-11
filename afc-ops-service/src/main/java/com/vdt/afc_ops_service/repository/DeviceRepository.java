package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsByDeviceCode(String deviceCode);

    Optional<Device> findByIdAndStationRouteOperatorId(Long id, Long operatorId);

    List<Device> findAllByStationOrderByDeviceCodeAsc(Station station);

    @Query("SELECT d.deviceCode FROM Device d " +
            "WHERE d.station = :station " +
            "AND d.deviceCode LIKE CONCAT(:prefix, '-%')")
    List<String> findDeviceCodesByStationAndPrefix(@Param("station") Station station,
                                                   @Param("prefix") String prefix);

    @Query(
            value = "SELECT d FROM Device d " +
                    "JOIN d.station s " +
                    "JOIN s.route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:deviceType IS NULL OR d.deviceType = :deviceType) " +
                    "AND (:status IS NULL OR d.status = :status) " +
                    "AND (:keywordPattern IS NULL OR LOWER(d.deviceCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationName) LIKE :keywordPattern)",
            countQuery = "SELECT COUNT(d) FROM Device d " +
                    "JOIN d.station s " +
                    "JOIN s.route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:stationId IS NULL OR s.id = :stationId) " +
                    "AND (:deviceType IS NULL OR d.deviceType = :deviceType) " +
                    "AND (:status IS NULL OR d.status = :status) " +
                    "AND (:keywordPattern IS NULL OR LOWER(d.deviceCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationCode) LIKE :keywordPattern " +
                    "OR LOWER(s.stationName) LIKE :keywordPattern)"
    )
    Page<Device> searchDevices(@Param("operatorId") Long operatorId,
                               @Param("stationId") Long stationId,
                               @Param("deviceType") String deviceType,
                               @Param("status") String status,
                               @Param("keywordPattern") String keywordPattern,
                               Pageable pageable);
}
