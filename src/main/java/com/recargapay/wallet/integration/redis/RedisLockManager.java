package com.recargapay.wallet.integration.redis;

import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@AllArgsConstructor
@Slf4j
public class RedisLockManager {
    //mutex
    private final static String WALLET_CREATION_MUTEX_TEMPLATE = "CREATING::WALLET::%s::%s";
    private final static String WALLET_DEBIT_MUTEX_TEMPLATE = "DEBIT::WALLET::%s";
    //error messages
    private static final String WALLET_CREATION_IN_PROGRESS = "Wallet creation already in progress";
    private static final String WALLET_DEBIT_IN_PROGRESS = "Wallet debit already in progress";
    //keys
    private final static String TRANSFER_NUMBER_KEY = "TRANSFER::NUMBER";
    private final static String WITHDRAWAL_NUMBER_KEY = "WITHDRAWAL::NUMBER";

    private static final int DEFAULT_TTL_SECONDS = 60;
    private static final String LOCKED_VALUE = "LOCKED";
    private final StringRedisTemplate redisTemplate;


    public Map<String, String> getAllEntries(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        val values = redisTemplate.opsForValue().multiGet(keys);
        val keyIterator = keys.iterator();
        val valueIterator = values.iterator();

        Map<String, String> result = new HashMap<>();
        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            result.put(keyIterator.next(), valueIterator.next());
        }

        return result;
    }

    public boolean setKeyValueIfNotExist(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long getNextTransferNumber() {
        return redisTemplate.opsForValue().increment(TRANSFER_NUMBER_KEY);
    }

    public Long getNextWithdrawalNumber() {
        return redisTemplate.opsForValue().increment(WITHDRAWAL_NUMBER_KEY);
    }

    public <T> T runWithCreateWalletLock(UUID userUuid, CurrencyType currency, Supplier<T> action) {
        val key = WALLET_CREATION_MUTEX_TEMPLATE.formatted(currency.name(), userUuid);
        if (!tryLock(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS))) {
            throw new WalletException(WALLET_CREATION_IN_PROGRESS, HttpStatus.CONFLICT, true);
        }
        return runWithLock(action, () -> releaseLock(key));
    }

    public <T> T runWithDebitFundsLock(UUID walletUuid, Supplier<T> action) {
        val key = WALLET_DEBIT_MUTEX_TEMPLATE.formatted(walletUuid);
        if (!tryLock(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS))) {
            throw new WalletException(WALLET_DEBIT_IN_PROGRESS, HttpStatus.CONFLICT, true);
        }
        return runWithLock(action, () -> releaseLock(key));
    }


    //private methods
    private <T> T runWithLock(Supplier<T> action, Runnable unlock) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    unlock.run();
                }
            });
            return action.get();
        } else {
            // fallback: unlock immediately after action
            try {
                return action.get();
            } finally {
                unlock.run();
            }
        }
    }

    private boolean tryLock(String key, Duration ttl) {
        val locked = redisTemplate.opsForValue().setIfAbsent(key, LOCKED_VALUE, ttl);
        log.debug("Key:{} locked:{}", key, locked);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseLock(String key) {
        val delete = redisTemplate.delete(key);
        log.debug("Key:{} released:{}", key, delete);
        log.debug("active keys:{}", redisTemplate.keys("*"));
    }

}
