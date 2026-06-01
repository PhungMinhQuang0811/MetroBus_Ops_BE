package com.vdt.authservice.modules.wallet.repository;

import com.vdt.authservice.modules.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    boolean existsByAccountIdAndWalletType(String accountId, String walletType);
    Optional<Wallet> findByAccountIdAndWalletType(String accountId, String walletType);
}
