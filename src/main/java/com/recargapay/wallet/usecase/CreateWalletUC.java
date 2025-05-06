package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletRsDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import com.recargapay.wallet.integration.http.corebanking.data.Status;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.service.UserService;
import com.recargapay.wallet.persistence.service.WalletService;
import com.recargapay.wallet.usecase.data.CurrencyType;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
@Validated
public class CreateWalletUC {
    // Error message constants
    private static final String WALLET_ALREADY_EXISTS_TEMPLATE = "User already has a wallet with currency:%s";

    private final RedisLockManager redisLockManager;
    private final UserService userService;
    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final CoreBankingClient coreBankingClient;
    private final JsonHelper jsonHelper;
    private final NotificationService notificationService;

    @Transactional
    public WalletRsDTO createWallet(@Valid CreateWalletRqDTO dto) {
        val userId = dto.userId();
        val currency = dto.currency();
        return redisLockManager.runWithCreateWalletLock(userId, currency, () -> {
            validateWalletOrThrow(userId, currency);
            val user = userService.getUserByUuid(userId);
            val response = coreBankingClient.createCvu(userId, dto.alias(), currency);
            if (Status.OK.equals(response.status())) {
                val walletDTO = walletMapper.toWalletDTO(walletService.saveNew(dto, user));
                sendPendingNotification(user.getEmail());
                return walletDTO;
            } else {
                val message = "Error creating destinationCvu, code:%s, details:%s".formatted(response.error().code(), response.error().details());
                log.warn("{} user:{} currency:{}", message, userId, currency);
                throw new WalletException(message, HttpStatus.CONFLICT, true);
            }
        });
    }

    @Transactional
    public void completeWalletCreation(CvuCreatedDTO dto) {
        val wallet = walletService.fetchPendingWalletBy(dto.userId(), dto.currency())
                .orElseThrow(() -> new WalletException("Wallet creation incomplete", HttpStatus.INTERNAL_SERVER_ERROR, false));
        walletService.updateWallet(wallet, dto);
        log.info("wallet creation done:{}", jsonHelper.serialize(wallet));
        sendCompleteNotification(wallet.getUser().getEmail());
    }

    //private methods
    private void validateWalletOrThrow(UUID userId, CurrencyType currency) {
        if (walletService.walletExistByUserAndCurrency(userId, currency)) {
            throw new WalletException(WALLET_ALREADY_EXISTS_TEMPLATE.formatted(currency), HttpStatus.CONFLICT, true);
        }
    }

    private void sendCompleteNotification(String email) {
        val id = notificationService.notifyWalletCompleted(email);
        log.info("WALLET_COMPLETE notification id:{} sending to:{}", id, email);
    }

    private void sendPendingNotification(String email) {
        val id = notificationService.notifyWalletPending(email);
        log.info("WALLET_PENDING notification id:{} sending to:{}", id, email);
    }
}
