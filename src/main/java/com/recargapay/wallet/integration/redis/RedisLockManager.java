package com.recargapay.wallet.integration.redis;

import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@AllArgsConstructor
@Slf4j
public class RedisLockManager {
    private final static String WALLET_CREATION_MUTEX_TEMPLATE = "CREATING::WALLET::%s::%s";
    private static final int DEFAULT_TTL_SECONDS = 60;
    private static final String LOCKED_VALUE = "LOCKED";
    private final StringRedisTemplate redisTemplate;

    public boolean tryLockCreateWallet(UUID userUuid, CurrencyType currency) {
        val key = WALLET_CREATION_MUTEX_TEMPLATE.formatted(currency.name(), userUuid);
        return tryLock(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS));
    }

    public void releaseCreateWallet(UUID userUuid, CurrencyType currency) {
        val key = WALLET_CREATION_MUTEX_TEMPLATE.formatted(currency.name(), userUuid);
        releaseLock(key);
    }

    public boolean setKeyValueIfNotExist(String key) {
        return tryLock(key);
    }


    public <T> T runWithLock(Supplier<T> action, Runnable unlock) {
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

    //private methods
    private boolean tryLock(String key, Duration ttl) {
        val locked = redisTemplate.opsForValue().setIfAbsent(key, LOCKED_VALUE, ttl);
        log.debug("Key:{} locked:{}", key, locked);
        return Boolean.TRUE.equals(locked);
    }

    private boolean tryLock(String key) {
        val locked = redisTemplate.opsForValue().setIfAbsent(key, LOCKED_VALUE);
        log.debug("Key:{} locked:{}, without ttl", key, locked);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseLock(String key) {
        val delete = redisTemplate.delete(key);
        log.debug("Key:{} released:{}", key, delete);
        log.debug("active keys:{}", redisTemplate.keys("*"));
    }

}
