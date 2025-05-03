package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRsDTO;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.integration.sqs.producer.SqsService;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.GenericMessage;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class MockCoreBankingClientImpl implements CoreBankingClient {
    private static final int CVU_LENGTH = 22;
    private static final Random RANDOM = new Random();
    private static final Set<UUID> blackListUsers = Set.of(UUID.fromString("4cea3c10-4c50-4c36-a7bc-ba17ec1a3f9c"));
    private final RedisLockManager redisLockManager;
    private final SqsService sqsService;
    @Value("${sqs.cvu-created.name}")
    private String cvuCreatedSqsName;

    private final static String ALIAS_TEMPLATE = "ALIAS_USED::%s";
    private final static String CVU_TEMPLATE = "CVU_USED::%s";
    private final static String CBU = "2850590940090418135201";

    @SneakyThrows
    @Override
    public CreateCvuRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        Thread.sleep(2000);
        CvuCreatedDTO dtoToSend;
        if (blackListUsers.contains(userId)) {
            dtoToSend = createRsUserBlacklistedByBcra(userId, currency);
        } else if (!canUseAlias(alias)) {
            return createRsAliasExist();
        } else {
            val cvu = generateUniqueCVU();
            dtoToSend = createRsOK(userId, alias, currency, cvu);
        }
        sqsService.doEnqueue(cvuCreatedSqsName, new GenericMessage<>(dtoToSend));
        return new CreateCvuRsDTO(CreateCvuRsDTO.Status.OK, null);
    }

    @Override
    public void withdrawal(UUID walletId, BigDecimal amount) {

    }

    //private methods
    private CvuCreatedDTO createRsOK(UUID userId, String alias, CurrencyType currency, String cvu) {
        return new CvuCreatedDTO(
                alias,
                cvu,
                CBU,//no mapping
                currency,
                WalletStatus.ACTIVE,
                null,
                userId);
    }

    private static CvuCreatedDTO createRsUserBlacklistedByBcra(UUID userId, CurrencyType currency) {
        return new CvuCreatedDTO(
                null,
                null,
                null,//no mapping
                currency,
                WalletStatus.REJECTED,
                new CvuCreatedDTO.Error("User blacklisted by BCRA", "002"),
                userId);
    }

    private static CreateCvuRsDTO createRsAliasExist() {
        return new CreateCvuRsDTO(
                CreateCvuRsDTO.Status.ERROR,
                new CreateCvuRsDTO.Error("001", "alias exist"));
    }


    private boolean canUseAlias(String alias) {
        val aliasKey = ALIAS_TEMPLATE.formatted(alias);
        return redisLockManager.setKeyValueIfNotExist(aliasKey);
    }

    private String generateUniqueCVU() {
        String cvu;
        do {
            cvu = generateRandomCVU();
        } while (!redisLockManager.setKeyValueIfNotExist(CVU_TEMPLATE.formatted(cvu)));
        return cvu;
    }

    private static String generateRandomCVU() {
        StringBuilder sb = new StringBuilder(CVU_LENGTH);
        for (int i = 0; i < CVU_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

}
