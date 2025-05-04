package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CoreBankingRsDTO;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.integration.sqs.producer.SqsService;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.usecase.data.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.GenericMessage;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_EXIST;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_NOT_EXIST_FOR_CASH_IN;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.BLACK_LISTED_USER;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.CVU_NOT_EXIST_FOR_CASH_IN;
import static java.util.Objects.isNull;

@RequiredArgsConstructor
public class MockCoreBankingClientImpl implements CoreBankingClient {
    private static final int CVU_LENGTH = 22;
    private static final Random RANDOM = new Random();
    private static final Set<UUID> blackListUsers = Set.of(UUID.fromString("4cea3c10-4c50-4c36-a7bc-ba17ec1a3f9c"));
    private final RedisLockManager redisLockManager;
    private final SqsService sqsService;
    @Value("${sqs.cvu-created.name}")
    private String cvuCreatedSqsName;
    @Value("${sqs.deposit-arrive.name}")
    private String depositArriveSqsName;

    private final static String ALIAS_TEMPLATE = "ALIAS_USED::%s";
    private final static String CVU_TEMPLATE = "CVU_USED::%s";
    private final static String CVU_BY_ALIAS_TEMPLATE = "CVU_BY_ALIAS::%s";
    private final static String CBU = "2850590940090418135201";

    @SneakyThrows
    @Override
    public CoreBankingRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        Thread.sleep(2000);
        CvuCreatedDTO dtoToSend;
        if (blackListUsers.contains(userId)) {
            dtoToSend = createRsUserBlacklistedByBcra(userId, currency);
        } else if (!canUseAlias(alias)) {
            return createErrorRs(ALIAS_EXIST);
        } else {
            val cvu = generateUniqueCVU(alias);
            dtoToSend = createRsOK(userId, alias, currency, cvu);
        }
        sqsService.doEnqueue(cvuCreatedSqsName, new GenericMessage<>(dtoToSend));
        return createOkRs();
    }

    @Override
    public CoreBankingRsDTO deposit(DepositSimulatedDTO rq) {
        if (!existAlias(rq.destinationAlias())) {
            return createErrorRs(ALIAS_NOT_EXIST_FOR_CASH_IN);
        }
        val cvu = getCvuBy(rq.destinationAlias());
        if (isNull(cvu)) {
            return createErrorRs(CVU_NOT_EXIST_FOR_CASH_IN);
        }
        val msj = new DepositArrivedDTO(rq.destinationAlias(), cvu, rq.amount(), rq.sourceCvu(), rq.sourceCbu(), rq.externalTxId());
        sqsService.doEnqueue(depositArriveSqsName, new GenericMessage<>(msj));
        return createOkRs();

    }

    private String getCvuBy(@NotBlank String alias) {
        val aliasKey = CVU_BY_ALIAS_TEMPLATE.formatted(alias);
        return redisLockManager.getValue(aliasKey);
    }

    @Override
    public void withdrawal(UUID walletId, BigDecimal amount) {

    }

    //private methods
    private static CoreBankingRsDTO createErrorRs(Error error) {
        return new CoreBankingRsDTO(
                CoreBankingRsDTO.Status.ERROR,
                new CoreBankingRsDTO.Error(error.code, error.details));
    }

    private static CoreBankingRsDTO createOkRs() {
        return new CoreBankingRsDTO(CoreBankingRsDTO.Status.OK, null);
    }

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
                new CvuCreatedDTO.Error(BLACK_LISTED_USER.details, BLACK_LISTED_USER.code),
                userId);
    }

    private boolean canUseAlias(String alias) {
        val aliasKey = ALIAS_TEMPLATE.formatted(alias);
        return redisLockManager.setKeyValueIfNotExist(aliasKey);
    }

    private boolean existAlias(String alias) {
        val aliasKey = ALIAS_TEMPLATE.formatted(alias);
        return redisLockManager.hasKey(aliasKey);
    }

    private String generateUniqueCVU(String alias) {
        String cvu;
        do {
            cvu = generateRandomCVU();
        } while (!redisLockManager.setKeyValueIfNotExist(CVU_TEMPLATE.formatted(cvu)));
        redisLockManager.setKeyValueIfNotExist(CVU_BY_ALIAS_TEMPLATE.formatted(alias), cvu);
        return cvu;
    }

    private static String generateRandomCVU() {
        StringBuilder sb = new StringBuilder(CVU_LENGTH);
        for (int i = 0; i < CVU_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    @Getter
    enum Error {
        ALIAS_EXIST("001", "destinationAlias already exist"),
        BLACK_LISTED_USER("002", "User blacklisted by BCRA"),
        ALIAS_NOT_EXIST_FOR_CASH_IN("003", "destinationAlias not exist, to transfer or deposit"),
        CVU_NOT_EXIST_FOR_CASH_IN("004", "destinationCvu not exist, to transfer or deposit");
        private final String code;
        private final String details;

        Error(String code, String details) {
            this.code = code;
            this.details = details;
        }
    }
}
