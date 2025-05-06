package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.TransferFundsRqDTO;
import com.recargapay.wallet.controller.data.TransferRsDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.mapper.TransactionMapper;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.service.TransactionService;
import com.recargapay.wallet.persistence.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
@Validated
public class TransferFundsUC {
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final RedisLockManager redisLockManager;
    private final NotificationService notificationService;
    private final TransactionMapper txMapper;

    @Transactional
    public TransferRsDTO transfer(@Valid TransferFundsRqDTO dto, @NotNull UUID walletId) {
        return redisLockManager.runWithDebitFundsLock(walletId, () -> {
            val source = walletService.fetchByUuidOrThrow(walletId);
            val destination = walletService.fetchByUuidOrThrow(dto.destinationWalletId());
            validateTransferOrThrow(dto, source, destination);
            val sourceTx = transactionService.saveTransfer(source, destination, dto.transferAmount());
            updateBalanceAndSave(dto, source, destination);
            val transferDTO = txMapper.toTransferDTO(sourceTx, dto.transferAmount());
            sendNotification(destination);
            return transferDTO;
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
        val id = notificationService.notifyTransferCompleted(email);
        log.info("DEPOSIT_COMPLETE notification id:{} sending to:{}", id, email);
    }

    private void updateBalanceAndSave(TransferFundsRqDTO dto, Wallet source, Wallet destination) {
        source.setBalance(source.getBalance().subtract(dto.transferAmount()));
        destination.setBalance(destination.getBalance().add(dto.transferAmount()));
        walletService.save(source, destination);
    }
}
