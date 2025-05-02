package com.recargapay.wallet.persistence.repository;

import com.recargapay.wallet.persistence.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> getByUser_Uuid(UUID userUuid);

    long countWalletByUser_UuidAndCurrency(UUID userUuid, String currency);

}
