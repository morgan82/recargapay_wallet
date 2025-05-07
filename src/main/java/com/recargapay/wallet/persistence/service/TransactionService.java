package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.WithdrawalCompleteDTO;
import com.recargapay.wallet.mapper.TransactionMapper;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.repository.TransactionRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recargapay.wallet.persistence.entity.TransactionStatus.COMPLETED;
import static com.recargapay.wallet.persistence.entity.TransactionStatus.PENDING;
import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
@Validated
@Slf4j
public class TransactionService {
    private final TransactionMapper txMapper;
    private final TransactionRepository transactionRepository;
    private final RedisLockManager redisLockManager;
    private final JsonHelper jsonHelper;

    public Transaction findByIdOrThrow(UUID txId) {
        return transactionRepository.getByUuid(txId)
                .orElseThrow(() -> new WalletException("Transaction %s not found".formatted(txId), HttpStatus.INTERNAL_SERVER_ERROR, false));
    }

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

    public Transaction saveWithdrawal(Wallet source, AccountInfoRsDTO destinationAccount, UUID txId, @NotNull @DecimalMin(value = "1.00") BigDecimal amount) {
        val tx = txMapper.fromWithdrawal(source, destinationAccount, txId, amount);
        return transactionRepository.save(tx);
    }

    public void updateWithdrawal(Transaction wdrltx, WithdrawalCompleteDTO dto) {
        txMapper.updateWithdrawal(wdrltx, dto);
        transactionRepository.save(wdrltx);
    }

    //private methods
    private boolean existsTx(Transaction tx) {
        val destinationAId = tx.getDestinationAccountId();
        val sourceAId = tx.getSourceAccountId();
        val externalTxId = tx.getExternalTxId();
        val walletUuid = tx.getWallet().getUuid();
        return transactionRepository.existTx(walletUuid, externalTxId, destinationAId, sourceAId) > 0;
    }

    public BigDecimal sumAmountBy(UUID walletId, LocalDateTime at) {
        val sumAmountBy = transactionRepository.sumAmountBy(walletId, List.of(COMPLETED, PENDING), at);
        return ofNullable(sumAmountBy).orElse(BigDecimal.ZERO);
    }
}
