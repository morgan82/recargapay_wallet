package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.TransferDTO;
import com.recargapay.wallet.controller.data.TransferFundsRqDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.service.TransactionService;
import com.recargapay.wallet.persistence.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class TransferFundsUC {
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final RedisLockManager redisLockManager;
    private final NotificationService notificationService;

    @Transactional
    public TransferDTO transfer(TransferFundsRqDTO dto) {
        return redisLockManager.runWithDebitFundsLock(dto.sourceWalletId(), () -> {
            val source = walletService.fetchByUuidOrThrow(dto.sourceWalletId());
            val destination = walletService.fetchByUuidOrThrow(dto.destinationWalletId());
            validateTransferOrThrow(dto, source, destination);
            val sourceTx = transactionService.saveTransfer(source, destination, dto.transferAmount());
            updateBalanceAndSave(dto, source, destination);
            sendNotification(destination);
            return mapToDTO(sourceTx, dto.transferAmount());
        });
    }

    //private methods
    private void validateTransferOrThrow(TransferFundsRqDTO dto, Wallet source, Wallet destination) {
        if (sameWallets(source, destination)) {
            throw new WalletException("Source and destination wallets must be different", HttpStatus.CONFLICT, true);
        }
        if (!sameCurrencies(source, destination)) {
            throw new WalletException("Currency mismatch between wallets", HttpStatus.CONFLICT, true);
        }
        if (!canDebit(dto, source)) {
            throw new WalletException("Insufficient funds", HttpStatus.CONFLICT, true);
        }
    }

    private boolean sameWallets(Wallet source, Wallet destination) {
        return source.getUuid().equals(destination.getUuid());
    }

    private static boolean sameCurrencies(Wallet source, Wallet destination) {
        return source.getCurrency().equals(destination.getCurrency());
    }

    private static boolean canDebit(TransferFundsRqDTO dto, Wallet source) {
        return source.getBalance().compareTo(dto.transferAmount()) >= 0;
    }

    private void sendNotification(Wallet destination) {
        val email = destination.getUser().getEmail();
        val id = notificationService.sendTransferComplete(email);
        log.info("DEPOSIT_COMPLETE notification id:{} sending to:{}", id, email);
    }

    private void updateBalanceAndSave(TransferFundsRqDTO dto, Wallet source, Wallet destination) {
        source.setBalance(source.getBalance().subtract(dto.transferAmount()));
        destination.setBalance(destination.getBalance().add(dto.transferAmount()));
        walletService.save(source, destination);
    }

    private TransferDTO mapToDTO(Transaction sourceTx, BigDecimal amount) {
        val destinationAccountId = UUID.fromString(sourceTx.getDestinationAccountId());
        val sourceWallet = sourceTx.getWallet();
        val walletSource = new TransferDTO.WalletSource(sourceWallet.getUuid(), sourceWallet.getBalance());
        val txStatus = sourceTx.getStatus();
        return new TransferDTO(destinationAccountId, amount, txStatus, walletSource);
    }
}
