package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    Optional<Operator> findByOperatorCode(String operatorCode);
}
