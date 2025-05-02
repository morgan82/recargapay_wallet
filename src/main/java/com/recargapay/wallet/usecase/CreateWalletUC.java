package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.mapper.WalletMapping;
import com.recargapay.wallet.persistence.service.UserService;
import com.recargapay.wallet.persistence.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CreateWalletUC {
    // Error message constants
    private static final String WALLET_CREATION_IN_PROGRESS = "Wallet creation already in progress";
    private static final String WALLET_ALREADY_EXISTS_TEMPLATE = "User already has a wallet with currency:%s";

    private RedisLockManager redisLockManager;
    private UserService userService;
    private WalletService walletService;
    private WalletMapping walletMapping;

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
            val newWallet = walletMapping.toNewWalletEntity(dto, user);
            val walletSaved = walletService.save(newWallet);
            return walletMapping.toWalletDTO(walletSaved);
        }, () -> redisLockManager.releaseCreateWallet(userId, currency));
    }
}
