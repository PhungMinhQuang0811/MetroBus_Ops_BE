package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByOperatorAndRouteCode(Operator operator, String routeCode);
}
