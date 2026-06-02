package com.vdt.authservice.modules.identity.repository;

import com.vdt.authservice.modules.identity.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByPhoneNumber(String phoneNumber);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.email = :identifier OR a.username = :identifier OR a.phoneNumber = :identifier")
    Optional<Account> findByIdentifier(@Param("identifier") String identifier);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE a.id = :id")
    Optional<Account> findById(@Param("id") String id);
}
