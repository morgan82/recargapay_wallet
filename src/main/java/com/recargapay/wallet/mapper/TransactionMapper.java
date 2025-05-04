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
    @Mapping(target = "wallet", source = "wallet")
    @Mapping(target = "sourceAccountId", source = "dto", qualifiedByName = "getSourceAccountId")
    @Mapping(target = "sourceAccountType", source = "dto", qualifiedByName = "getSourceAccountType")
    @Mapping(target = "destinationAccountType", expression = "java( AccountType.BANK_CVU)")
    @Mapping(target = "destinationAccountId", source = "wallet.cvu")
    Transaction fromDeposit(DepositArrivedDTO dto, Wallet wallet);

    @Named("getSourceAccountId")
    default String getSourceAccountId(DepositArrivedDTO dto) {
        return isNotBlank(dto.sourceCbu()) ? dto.sourceCbu() : dto.sourceCvu();
    }

    @Named("getSourceAccountType")
    default AccountType getSourceAccountType(DepositArrivedDTO dto) {
        return isNotBlank(dto.sourceCbu()) ? AccountType.BANK_CBU : AccountType.BANK_CVU;
    }
}
