package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
