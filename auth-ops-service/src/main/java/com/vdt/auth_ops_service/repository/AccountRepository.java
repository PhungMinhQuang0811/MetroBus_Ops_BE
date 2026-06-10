package com.vdt.auth_ops_service.repository;

import com.vdt.auth_ops_service.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByUsername(String username);

    Optional<Account> findByUsernameAndOperatorCode(String username, String operatorCode);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.username = :username")
    Optional<Account> findByUsername(@Param("username") String username);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.id = :id")
    Optional<Account> findById(@Param("id") String id);

    @Query(
            value = "SELECT DISTINCT a FROM Account a " +
                    "LEFT JOIN a.roles r " +
                    "WHERE a.operatorCode = :operatorCode " +
                    "AND LOWER(a.username) LIKE :keywordPattern " +
                    "AND (:role IS NULL OR r.name = :role) " +
                    "AND (:isActive IS NULL OR a.isActive = :isActive) " +
                    "AND (:passwordStatus IS NULL OR a.passwordStatus = :passwordStatus)",
            countQuery = "SELECT COUNT(DISTINCT a) FROM Account a " +
                    "LEFT JOIN a.roles r " +
                    "WHERE a.operatorCode = :operatorCode " +
                    "AND LOWER(a.username) LIKE :keywordPattern " +
                    "AND (:role IS NULL OR r.name = :role) " +
                    "AND (:isActive IS NULL OR a.isActive = :isActive) " +
                    "AND (:passwordStatus IS NULL OR a.passwordStatus = :passwordStatus)"
    )
    Page<Account> searchAccounts(@Param("operatorCode") String operatorCode,
                                 @Param("keywordPattern") String keywordPattern,
                                 @Param("role") String role,
                                 @Param("isActive") Boolean isActive,
                                 @Param("passwordStatus") String passwordStatus,
                                 Pageable pageable);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.id = :id AND a.operatorCode = :operatorCode")
    Optional<Account> findByIdAndOperatorCode(@Param("id") String id, @Param("operatorCode") String operatorCode);

    @Query("SELECT COUNT(DISTINCT a) FROM Account a JOIN a.roles r " +
            "WHERE a.isActive = true AND r.name = :roleName")
    long countActiveAccountsByRoleName(@Param("roleName") String roleName);
}
