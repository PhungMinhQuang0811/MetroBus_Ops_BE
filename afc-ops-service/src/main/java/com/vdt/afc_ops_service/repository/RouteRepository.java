package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByOperatorAndRouteCode(Operator operator, String routeCode);

    Optional<Route> findByIdAndOperator(Long id, Operator operator);

    @Query("SELECT r.routeCode FROM Route r " +
            "WHERE r.operator = :operator " +
            "AND r.routeCode LIKE CONCAT(:prefix, '-%')")
    List<String> findRouteCodesByOperatorAndPrefix(@Param("operator") Operator operator,
                                                   @Param("prefix") String prefix);

    @Query(
            value = "SELECT r FROM Route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:keywordPattern IS NULL OR LOWER(r.routeCode) LIKE :keywordPattern " +
                    "OR LOWER(r.routeName) LIKE :keywordPattern) " +
                    "AND (:transportType IS NULL OR r.transportType = :transportType) " +
                    "AND (:status IS NULL OR r.status = :status)",
            countQuery = "SELECT COUNT(r) FROM Route r " +
                    "WHERE r.operator.id = :operatorId " +
                    "AND (:keywordPattern IS NULL OR LOWER(r.routeCode) LIKE :keywordPattern " +
                    "OR LOWER(r.routeName) LIKE :keywordPattern) " +
                    "AND (:transportType IS NULL OR r.transportType = :transportType) " +
                    "AND (:status IS NULL OR r.status = :status)"
    )
    Page<Route> searchRoutes(@Param("operatorId") Long operatorId,
                             @Param("keywordPattern") String keywordPattern,
                             @Param("transportType") String transportType,
                             @Param("status") String status,
                             Pageable pageable);
}
