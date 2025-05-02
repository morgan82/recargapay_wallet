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
    private final StringRedisTemplate redisTemplate;
    private final static String CREATE = "CREATING";
    private final static String WALLET_KEY_TEMPLATE = "%s::WALLET::%s::%s";

    public boolean tryLockCreateWallet(UUID userUuid, CurrencyType currency) {
        val key = WALLET_KEY_TEMPLATE.formatted(CREATE, currency.name(), userUuid);
        return tryLock(key, Duration.ofSeconds(10));
    }

    public void releaseCreateWallet(UUID userUuid, CurrencyType currency) {
        val key = WALLET_KEY_TEMPLATE.formatted(CREATE, currency.name(), userUuid);
        releaseLock(key);
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
        val locked = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", ttl);
        log.debug("Key locked:{}", locked);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseLock(String key) {
        val delete = redisTemplate.delete(key);
        log.debug("Key released:{}", delete);
    }


}
