package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedRqDTO;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.DefaultRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.Status;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CoreBankingError;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.WithdrawalCompleteDTO;
import com.recargapay.wallet.integration.sqs.producer.SqsService;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.usecase.data.CurrencyType;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_EXIST;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_NOT_EXIST_FOR_CASH_IN;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.BLACK_LISTED_USER;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.CVU_NOT_EXIST_FOR_CASH_IN;
import static com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO.BankAccountType.CBU;
import static com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO.BankAccountType.CVU;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class MockCoreBankingClientImpl implements CoreBankingClient {
    private static final int CVBU_LENGTH = 22;
    private static final Random RANDOM = new Random();
    private static final Set<UUID> blackListUsers = Set.of(UUID.fromString("4cea3c10-4c50-4c36-a7bc-ba17ec1a3f9c"));
    private static final String ALIAS_ERROR_OUT = "test.error.out";
    private static final HashMap<String, AccountInfoRsDTO> existingAccountByAlias = new HashMap<>();
    private final RedisLockManager redisLockManager;
    private final SqsService sqsService;
    @Value("${sqs.cvu-created.name}")
    private String cvuCreatedSqsName;
    @Value("${sqs.deposit-arrive.name}")
    private String depositArriveSqsName;
    @Value("${sqs.withdrawal-complete.name}")
    private String withdrawalCompleteSqsName;

    private final static String ALIAS_TEMPLATE = "ALIAS_USED::%s";
    private final static String CVBU_TEMPLATE = "CVBU_USED::%s";
    private final static String CVBU_BY_ALIAS_TEMPLATE = "CVBU_BY_ALIAS::%s";
    private final static String RECARCAPAY_CBU = "2850590940090418135201";

    @SneakyThrows
    @Override
    public DefaultRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        Thread.sleep(2000);//simulate work
        CvuCreatedDTO dtoToSend;
        if (blackListUsers.contains(userId)) {
            dtoToSend = cvuCreatedBlacklistedByBcra(userId, currency);
        } else if (!canUseAlias(alias)) {
            return createErrorRs(ALIAS_EXIST);
        } else {
            val cvu = generateUniqueCVBU(alias, true, true);
            dtoToSend = cvuCreatedRsOK(userId, alias, currency, cvu);
        }
        sqsService.doEnqueue(cvuCreatedSqsName, new GenericMessage<>(dtoToSend));
        return createOkRs();
    }

    @Override
    public DefaultRsDTO deposit(DepositSimulatedRqDTO rq) {
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

    @SneakyThrows
    @Override
    public DefaultRsDTO withdrawal(String sourceCvu, UUID txId, String destinationCvbu, BigDecimal amount) {
        Thread.sleep(2000);//simulate work
        val destinationAccount = infoByCvbu(destinationCvbu);
        if (Status.ERROR == destinationAccount.status()) {
            return createErrorRs(Error.UNEXPECTED);
        }
        val externalTxId = "external-tx-%s".formatted(redisLockManager.getNextWithdrawalNumber());
        CoreBankingError error = null;
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        if (ALIAS_ERROR_OUT.equals(destinationAccount.alias())) {//simulate error
            error = new CoreBankingError(Error.BANK_ERROR_WITHDRAWAL.details, Error.BANK_ERROR_WITHDRAWAL.code);
            transactionStatus = TransactionStatus.FAILED;
        }
        val msj = new WithdrawalCompleteDTO(txId, destinationCvbu, destinationAccount.alias(),
                amount, externalTxId, transactionStatus, error);
        sqsService.doEnqueue(withdrawalCompleteSqsName, new GenericMessage<>(msj));
        if (destinationAccount.isRpUser()) {
            val msj2 = new DepositArrivedDTO(destinationAccount.alias(), destinationAccount.cvbu(),
                    amount, sourceCvu, null, externalTxId);
            sqsService.doEnqueue(depositArriveSqsName, new GenericMessage<>(msj2));
        }
        return createOkRs();
    }

    @Override
    public AccountInfoRsDTO infoByAlias(String alias) {
        return ofNullable(existingAccountByAlias.get(alias))
                .orElse(new AccountInfoRsDTO(null, null, null, Status.ERROR, null));
    }

    @Override
    public AccountInfoRsDTO infoByCvbu(String cvbu) {
        return existingAccountByAlias.values().stream()
                .filter(info -> info.cvbu().equals(cvbu))
                .findAny()
                .orElse(new AccountInfoRsDTO(null, null, null, Status.ERROR, null));
    }

    //private methods

    private String getCvuBy(@NotBlank String alias) {
        val aliasKey = CVBU_BY_ALIAS_TEMPLATE.formatted(alias);
        return redisLockManager.getValue(aliasKey);
    }

    private static DefaultRsDTO createErrorRs(Error error) {
        return new DefaultRsDTO(
                Status.ERROR,
                new DefaultRsDTO.Error(error.code, error.details));
    }

    private static DefaultRsDTO createOkRs() {
        return new DefaultRsDTO(Status.OK, null);
    }

    private static CvuCreatedDTO cvuCreatedRsOK(UUID userId, String alias, CurrencyType currency, String cvu) {
        return new CvuCreatedDTO(
                alias,
                cvu,
                RECARCAPAY_CBU,//no mapping
                currency,
                WalletStatus.ACTIVE,
                null,
                userId);
    }

    private static CvuCreatedDTO cvuCreatedBlacklistedByBcra(UUID userId, CurrencyType currency) {
        return new CvuCreatedDTO(
                null,
                null,
                null,//no mapping
                currency,
                WalletStatus.REJECTED,
                new CoreBankingError(BLACK_LISTED_USER.details, BLACK_LISTED_USER.code),
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

    private String generateUniqueCVBU(String alias, boolean isCVU, boolean isRpUser) {
        String cvbu;
        do {
            cvbu = generateRandomCVBU();
        } while (!redisLockManager.setKeyValueIfNotExist(CVBU_TEMPLATE.formatted(cvbu)));
        redisLockManager.setKeyValueIfNotExist(CVBU_BY_ALIAS_TEMPLATE.formatted(alias), cvbu);
        existingAccountByAlias.putIfAbsent(alias, new AccountInfoRsDTO(cvbu, alias, isCVU ? CVU : CBU, Status.OK, isRpUser));
        return cvbu;
    }

    private static String generateRandomCVBU() {
        StringBuilder sb = new StringBuilder(CVBU_LENGTH);
        for (int i = 0; i < CVBU_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    @Getter
    enum Error {
        ALIAS_EXIST("001", "alias already exist"),
        BLACK_LISTED_USER("002", "User blacklisted by BCRA"),
        ALIAS_NOT_EXIST_FOR_CASH_IN("003", "alias not exist, to transfer or deposit"),
        CVU_NOT_EXIST_FOR_CASH_IN("004", "cvu not exist, to transfer or deposit"),
        BANK_ERROR_WITHDRAWAL("005", "withdrawal failed: issue with destination bank"),
        UNEXPECTED("006", "unexpected error in core banking"),
        ;
        private final String code;
        private final String details;

        Error(String code, String details) {
            this.code = code;
            this.details = details;
        }
    }

    @PostConstruct
    private void init() {
        // Preloaded accounts inside RecargaPay (expected to already exist in Redis)
        val alias1 = "test.1ars.rp";
        val alias2 = "test.1usd.rp";
        val cvuCbu1 = redisLockManager.getValue(CVBU_BY_ALIAS_TEMPLATE.formatted(alias1));
        val cvuCbu2 = redisLockManager.getValue(CVBU_BY_ALIAS_TEMPLATE.formatted(alias2));
        Assert.isTrue(ObjectUtils.allNotNull(cvuCbu1, cvuCbu2), "cvu/cbu not exist, init error");
        existingAccountByAlias.putIfAbsent(alias1, new AccountInfoRsDTO(cvuCbu1, alias1, CVU, Status.OK, true));
        existingAccountByAlias.putIfAbsent(alias2, new AccountInfoRsDTO(cvuCbu2, alias2, CVU, Status.OK, true));
        // New external accounts (CVUs/CBUs are generated dynamically)
        val alias3 = "test.1ars.out";
        generateUniqueCVBU(alias3, false, false);
        val alias4 = "test.1usd.out";
        generateUniqueCVBU(alias4, false, false);
        val alias5 = "test.2ars.out";
        generateUniqueCVBU(alias5, false, false);
        val alias6 = "test.2usd.out";
        generateUniqueCVBU(alias6, false, false);
        generateUniqueCVBU(ALIAS_ERROR_OUT, false, false);
    }
}
