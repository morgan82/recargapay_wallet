package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRsDTO;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.mapper.WalletMapping;
import com.recargapay.wallet.persistence.service.UserService;
import com.recargapay.wallet.persistence.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class CreateWalletUC {
    // Error message constants
    private static final String WALLET_CREATION_IN_PROGRESS = "Wallet creation already in progress";
    private static final String WALLET_ALREADY_EXISTS_TEMPLATE = "User already has a wallet with currency:%s";

    private final RedisLockManager redisLockManager;
    private final UserService userService;
    private final WalletService walletService;
    private final WalletMapping walletMapping;
    private final CoreBankingClient coreBankingClient;
    private final JsonHelper jsonHelper;

    @Transactional
    public WalletDTO createWallet(CreateWalletRqDTO dto) {
        val userId = dto.userId();
        val currency = dto.currency();
        return redisLockManager.runWithLock(() -> {
            if (!redisLockManager.tryLockCreateWallet(userId, currency)) {
                throw new WalletException(WALLET_CREATION_IN_PROGRESS, HttpStatus.CONFLICT, true);
            }
            if (walletService.walletExistByUserAndCurrency(userId, currency)) {
                throw new WalletException(WALLET_ALREADY_EXISTS_TEMPLATE.formatted(currency), HttpStatus.CONFLICT, true);
            }
            val user = userService.getUserByUuid(userId);
            val response = coreBankingClient.createCvu(userId, dto.alias(), currency);
            if (CreateCvuRsDTO.Status.OK.equals(response.status())) {
                val newWallet = walletMapping.toNewWalletEntity(dto, user);
                val walletSaved = walletService.save(newWallet);
                return walletMapping.toWalletDTO(walletSaved);
            } else {
                val message = "Error, code:%s, details:%s".formatted(response.error().code(), response.error().details());
                throw new WalletException(message, HttpStatus.CONFLICT, true);
            }
        }, () -> redisLockManager.releaseCreateWallet(userId, currency));
    }

    @Transactional
    public void finalizeWalletCreation(CvuCreatedDTO dto) {
        val wallet = walletService.fetchWalletByUserIfExist(dto.userId(), dto.currency())
                .orElseThrow(() -> new WalletException("Wallet creation incomplete", HttpStatus.INTERNAL_SERVER_ERROR, false));
        walletMapping.updateWallet(wallet, dto);
        walletService.save(wallet);
        log.info("wallet creation done:{}", jsonHelper.serialize(wallet));
    }
}
