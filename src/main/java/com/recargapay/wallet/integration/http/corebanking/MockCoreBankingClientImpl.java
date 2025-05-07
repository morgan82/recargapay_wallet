package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedRqDTO;
import com.recargapay.wallet.helper.JsonHelper;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.GenericMessage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_EXIST;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.ALIAS_NOT_EXIST_FOR_CASH_IN;
import static com.recargapay.wallet.integration.http.corebanking.MockCoreBankingClientImpl.Error.BLACK_LISTED_USER;
import static com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO.BankAccountType.CBU;
import static com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO.BankAccountType.CVU;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class MockCoreBankingClientImpl implements CoreBankingClient {
    private static final int CVBU_LENGTH = 22;
    private static final Random RANDOM = new Random();
    private static final Set<UUID> blackListUsers = Set.of(UUID.fromString("4cea3c10-4c50-4c36-a7bc-ba17ec1a3f9c"));
    private static final String ALIAS_ERROR_WDRL = "test.error.out";
    private static final int TIME_SIMULATE_WORKING = 3000;
    private final RedisLockManager redisLockManager;
    private final SqsService sqsService;
    private final JsonHelper jsonHelper;
    @Value("${sqs.cvu-created.name}")
    private String cvuCreatedSqsName;
    @Value("${sqs.deposit-arrive.name}")
    private String depositArriveSqsName;
    @Value("${sqs.withdrawal-complete.name}")
    private String withdrawalCompleteSqsName;

    private final static String ACCOUNT_BY_ALIAS = "ALIAS::%s";
    private final static String ACCOUNT_BY_CVBU = "CVBU::%s";
    private final static String RECARCAPAY_CBU = "2850590940090418135201";

    @SneakyThrows
    @Override
    public DefaultRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        Thread.sleep(TIME_SIMULATE_WORKING);//simulate work
        CvuCreatedDTO dtoToSend;
        if (blackListUsers.contains(userId)) {
            dtoToSend = cvuCreatedBlacklistedByBcra(userId, currency);
        } else if (existAlias(alias)) {
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
        val cvu = getAccountByAlias(rq.destinationAlias()).cvbu();
        val msj = new DepositArrivedDTO(rq.destinationAlias(), cvu, rq.amount(), rq.sourceCvu(), rq.sourceCbu(), rq.externalTxId());
        sqsService.doEnqueue(depositArriveSqsName, new GenericMessage<>(msj));
        return createOkRs();

    }

    @SneakyThrows
    @Override
    public DefaultRsDTO withdrawal(String sourceCvu, UUID txId, String destinationCvbu, BigDecimal amount) {
        Thread.sleep(TIME_SIMULATE_WORKING);//simulate work
        val destinationAccount = getAccountByCvbu(destinationCvbu);
        if (Status.ERROR == destinationAccount.status()) {
            return createErrorRs(Error.UNEXPECTED);
        }
        val externalTxId = "external-tx-%s".formatted(redisLockManager.getNextWithdrawalNumber());
        CoreBankingError error = null;
        TransactionStatus transactionStatus = TransactionStatus.COMPLETED;
        if (ALIAS_ERROR_WDRL.equals(destinationAccount.alias())) {//simulate error
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
        return ofNullable(getAccountByAlias(alias))
                .orElse(new AccountInfoRsDTO(null, null, null, Status.ERROR, null));
    }

    @Override
    public AccountInfoRsDTO infoByCvbu(String cvbu) {
        return ofNullable(getAccountByCvbu(cvbu))
                .orElse(new AccountInfoRsDTO(null, null, null, Status.ERROR, null));
    }

    @Override
    public Map<String, AccountInfoRsDTO> listAccounts() {
        val pattern = "ALIAS::*";
        Map<String, String> allEntries = redisLockManager.getAllEntries(pattern);
        return allEntries.entrySet().stream().collect(toMap(
                Map.Entry::getKey,
                entry -> jsonHelper.parse(entry.getValue(), AccountInfoRsDTO.class)
        ));
    }

    //private methods

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

    private String generateUniqueCVBU(String alias, boolean isCVU, boolean isRpUser) {
        String cvbu;
        do {
            cvbu = generateRandomCVBU();
        } while (!putAccountIfAbsent(alias, cvbu, isCVU ? CVU : CBU, Status.OK, isRpUser));
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

    private boolean putAccountIfAbsent(String alias, String cvbu, AccountInfoRsDTO.BankAccountType bankAccountType, Status status, boolean isRpUser) {
        val accountInfoRsDTO = new AccountInfoRsDTO(cvbu, alias, bankAccountType, status, isRpUser);
        val objString = jsonHelper.serialize(accountInfoRsDTO);
        val aliasKey = ACCOUNT_BY_ALIAS.formatted(alias);
        redisLockManager.setKeyValueIfNotExist(aliasKey, objString);
        return redisLockManager.setKeyValueIfNotExist(ACCOUNT_BY_CVBU.formatted(cvbu), alias);
    }

    private boolean existAlias(String alias) {
        return redisLockManager.hasKey(ACCOUNT_BY_ALIAS.formatted(alias));
    }

    private AccountInfoRsDTO getAccountByAlias(String alias) {
        val objString = redisLockManager.getValue(ACCOUNT_BY_ALIAS.formatted(alias));
        return ofNullable(objString).map(obj -> jsonHelper.parse(obj, AccountInfoRsDTO.class))
                .orElse(null);
    }

    private AccountInfoRsDTO getAccountByCvbu(String cvbu) {
        val aliasKey = redisLockManager.getValue(ACCOUNT_BY_CVBU.formatted(cvbu));
        return ofNullable(aliasKey)
                .map(obj -> getAccountByAlias(aliasKey))
                .orElse(null);
    }

    @PostConstruct
    private void init() {
        // New external accounts (CVUs/CBUs are generated dynamically)
        generateUniqueCVBU("test.1ars.out", false, false);
        generateUniqueCVBU("test.2ars.out", false, false);
        generateUniqueCVBU("test.3ars.out", false, false);
        generateUniqueCVBU("test.4ars.out", false, false);
        generateUniqueCVBU("test.5ars.out", false, false);
        generateUniqueCVBU("test.6ars.out", false, false);
        generateUniqueCVBU("test.7ars.out", false, false);
        generateUniqueCVBU(ALIAS_ERROR_WDRL, false, false);
    }
}
