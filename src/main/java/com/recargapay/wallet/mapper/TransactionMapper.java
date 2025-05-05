package com.recargapay.wallet.mapper;

import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.persistence.entity.AccountType;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import com.recargapay.wallet.persistence.entity.TransactionType;
import com.recargapay.wallet.persistence.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = {UUID.class, TransactionStatus.class, AccountType.class, TransactionType.class})
public interface TransactionMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "extraInfo", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)

    @Mapping(target = "externalTxId", source = "dto.externalTxId")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "transactionType", expression = "java(TransactionType.DEPOSIT)")
    @Mapping(target = "status", expression = "java(TransactionStatus.COMPLETED)")
    @Mapping(target = "amount", source = "dto.amount")
    @Mapping(target = "wallet", source = "destination")
    @Mapping(target = "sourceAccountId", source = "dto", qualifiedByName = "getSourceAccountId")
    @Mapping(target = "sourceAccountType", source = "dto", qualifiedByName = "getSourceAccountType")
    @Mapping(target = "destinationAccountType", expression = "java( AccountType.BANK_CVU)")
    @Mapping(target = "destinationAccountId", source = "destination.cvu")
    Transaction fromDeposit(DepositArrivedDTO dto, Wallet destination);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "extraInfo", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)

    @Mapping(target = "externalTxId", expression = "java(externalTxIdForTransfer(source,destination,transferNumber))")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "transactionType", expression = "java(TransactionType.TRANSFER)")
    @Mapping(target = "status", expression = "java(TransactionStatus.COMPLETED)")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "debitAmount")
    @Mapping(target = "wallet", source = "source")
    @Mapping(target = "sourceAccountId", source = "source.uuid")
    @Mapping(target = "sourceAccountType", expression = "java(AccountType.WALLET_UUID)")
    @Mapping(target = "destinationAccountId", source = "destination.uuid")
    @Mapping(target = "destinationAccountType", expression = "java( AccountType.WALLET_UUID)")
    Transaction fromTransferSource(Wallet source, Wallet destination, BigDecimal amount, Long transferNumber);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "extraInfo", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)

    @Mapping(target = "externalTxId", expression = "java(externalTxIdForTransfer(source,destination,transferNumber))")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "transactionType", expression = "java(TransactionType.TRANSFER)")
    @Mapping(target = "status", expression = "java(TransactionStatus.COMPLETED)")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "wallet", source = "destination")
    @Mapping(target = "sourceAccountId", source = "source.uuid")
    @Mapping(target = "sourceAccountType", expression = "java(AccountType.WALLET_UUID)")
    @Mapping(target = "destinationAccountId", source = "destination.uuid")
    @Mapping(target = "destinationAccountType", expression = "java( AccountType.WALLET_UUID)")
    Transaction fromTransferDestination(Wallet source, Wallet destination, BigDecimal amount, Long transferNumber);

    @Named("getSourceAccountId")
    default String getSourceAccountId(DepositArrivedDTO dto) {
        return isNotBlank(dto.sourceCbu()) ? dto.sourceCbu() : dto.sourceCvu();
    }

    @Named("getSourceAccountType")
    default AccountType getSourceAccountType(DepositArrivedDTO dto) {
        return isNotBlank(dto.sourceCbu()) ? AccountType.BANK_CBU : AccountType.BANK_CVU;
    }

    default String externalTxIdForTransfer(Wallet source, Wallet destination, Long transferNumber) {
        return "%s::%s::%s".formatted(source.getCvu(), destination.getCvu(), transferNumber);
    }

    @Named("debitAmount")
    default BigDecimal debitAmount(BigDecimal amount) {
        return amount.negate();
    }
}
