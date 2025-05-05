package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.mapper.TransactionMapper;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.repository.TransactionRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Validated
@Slf4j
public class TransactionService {
    private TransactionMapper txMapper;
    private TransactionRepository transactionRepository;
    private RedisLockManager redisLockManager;
    private JsonHelper jsonHelper;

    public Optional<Transaction> saveDeposit(DepositArrivedDTO dto, Wallet source) {
        val tx = txMapper.fromDeposit(dto, source);
        if (existsTx(tx)) {
            log.warn("TRANSACTION IS DUPLICATE, skipping transaction {}", jsonHelper.serialize(tx));
            return Optional.empty();
        }
        return Optional.of(transactionRepository.save(tx));
    }

    public Transaction saveTransfer(Wallet source, Wallet destination, @NotNull @DecimalMin(value = "1.00") BigDecimal amount) {
        val transferNumber = redisLockManager.getNextTransferNumber();
        val sourceTx = txMapper.fromTransferSource(source, destination, amount, transferNumber);
        val destinationTx = txMapper.fromTransferDestination(source, destination, amount, transferNumber);
        transactionRepository.saveAll(List.of(sourceTx, destinationTx));
        return sourceTx;
    }

    //private methods
    private boolean existsTx(Transaction tx) {
        val destinationAId = tx.getDestinationAccountId();
        val sourceAId = tx.getSourceAccountId();
        val externalTxId = tx.getExternalTxId();
        val walletUuid = tx.getWallet().getUuid();
        return transactionRepository.existTx(walletUuid, externalTxId, destinationAId, sourceAId) > 0;
    }
}
