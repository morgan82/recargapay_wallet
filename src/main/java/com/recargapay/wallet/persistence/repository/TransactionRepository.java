package com.recargapay.wallet.persistence.repository;

import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> getByUuid(UUID uuid);

    @Query("""
                select count(t)  from Transaction t
                where t.wallet.uuid =:walletUuid
                and t.externalTxId=:externalTxId
                and t.destinationAccountId=:destinationAId
                and t.sourceAccountId=:sourceAId
            """)
    int existTx(@Param("walletUuid") UUID walletUuid,
                @Param("externalTxId") String externalTxId,
                @Param("destinationAId") String destinationAId,
                @Param("sourceAId") String sourceAId);

    @Query("""
            select sum(t.amount) from Transaction t
            where t.wallet.uuid =:walletUuid
            and t.status in :txStatuses
            and t.createdDate <=:at
            """)
    BigDecimal sumAmountBy(@Param("walletUuid") UUID walletUuid,
                           @Param("txStatuses") List<TransactionStatus> txStatuses,
                           @Param("at") LocalDateTime at);
}
