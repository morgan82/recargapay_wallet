package com.recargapay.wallet.persistence.repository;

import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("""
            select count(*) from Wallet w
            where w.user.uuid = :userUuid
            and w.status in :statuses
            and w.currency = :currency
            """)
    long countWalletBy(@Param("userUuid") UUID userUuid,
                       @Param("currency") String currency,
                       @Param("statuses") List<WalletStatus> statuses
    );

    Optional<Wallet> getByUser_UuidAndCurrencyAndStatus(UUID userUuid, String currency, WalletStatus status);

    @Query("""
            select w from Wallet w
            where w.status = :status
            and (w.cvu=:cvu or w.alias=:alias)
            """)
    Optional<Wallet> getBy(@Param("status") WalletStatus status,
                           @Param("cvu") String cvu,
                           @Param("alias") String alias);
}
