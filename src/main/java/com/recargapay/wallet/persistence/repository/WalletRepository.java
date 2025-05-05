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
    Optional<Wallet> getByUuid(UUID uuid);

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

    @Query("""
            select w from Wallet w
            where w.user.uuid = :userUuid
            and w.currency =:currency
            and w.status='PENDING'
            """)
    Optional<Wallet> getPendingBy(@Param("userUuid") UUID userUuid,
                                  @Param("currency") String currency);

    @Query("""
            select w from Wallet w
            where (w.cvu=:cvu or w.alias=:alias)
            and w.status='ACTIVE'
            """)
    Optional<Wallet> getActiveByCvuAndAlias(@Param("cvu") String cvu,
                                            @Param("alias") String alias);

    @Query("""
                select w from Wallet w
                where w.user.username = :username
                and w.currency = :currency
                and w.status = 'ACTIVE'
            """)
    Optional<Wallet> getActiveByUsernameAndCurrency(@Param("username") String username,
                                                    @Param("currency") String currency);

}
