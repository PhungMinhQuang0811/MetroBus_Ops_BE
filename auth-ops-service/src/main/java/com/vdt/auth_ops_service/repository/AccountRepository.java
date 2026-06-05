package com.vdt.auth_ops_service.repository;

import com.vdt.auth_ops_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByUsername(String username);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.username = :identifier")
    Optional<Account> findByIdentifier(@Param("identifier") String identifier);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.id = :id")
    Optional<Account> findById(@Param("id") String id);
}
