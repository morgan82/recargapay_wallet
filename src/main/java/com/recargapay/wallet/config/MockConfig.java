package com.recargapay.wallet.config;

import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.producer.SqsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockConfig {
    @Bean
    @ConditionalOnMissingBean
    public CoreBankingClient mockCoreBankingClient(RedisLockManager redisLockManager, SqsService sqsService, JsonHelper jsonHelper) {
        return new MockCoreBankingClientImpl(redisLockManager, sqsService, jsonHelper);
    }
}
