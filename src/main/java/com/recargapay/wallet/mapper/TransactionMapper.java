package com.recargapay.wallet.mapper;

import com.recargapay.wallet.controller.data.TransferRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.WithdrawalCompleteDTO;
import com.recargapay.wallet.persistence.entity.AccountType;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import com.recargapay.wallet.persistence.entity.TransactionType;
import com.recargapay.wallet.persistence.entity.Wallet;
import lombok.val;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "extraInfo", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "externalTxId", ignore = true)

    @Mapping(target = "uuid", source = "txId")
    @Mapping(target = "transactionType", expression = "java(TransactionType.WITHDRAWAL)")
    @Mapping(target = "status", expression = "java(TransactionStatus.PENDING)")
    @Mapping(target = "amount", source = "amount", qualifiedByName = "debitAmount")
    @Mapping(target = "wallet", source = "source")
    @Mapping(target = "sourceAccountId", source = "source.cvu")
    @Mapping(target = "sourceAccountType", expression = "java(AccountType.BANK_CVU)")
    @Mapping(target = "destinationAccountId", source = "destinationAccount.cvbu")
    @Mapping(target = "destinationAccountType", source = "destinationAccount", qualifiedByName = "destinationAccountType")
    Transaction fromWithdrawal(Wallet source, AccountInfoRsDTO destinationAccount, UUID txId, BigDecimal amount);


    default void updateWithdrawal(Transaction txWithdrawal, WithdrawalCompleteDTO dto) {
        txWithdrawal.setStatus(dto.transactionStatus());
        txWithdrawal.setExternalTxId(dto.externalTxId());
    }

    @Mapping(target = "destinationWalletId", expression = "java(UUID.fromString(sourceTx.getDestinationAccountId()))")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "txStatus", source = "sourceTx.status")
    @Mapping(target = "sourceWallet", source = "sourceTx", qualifiedByName = "transferSourceWallet")
    TransferRsDTO toTransferDTO(Transaction sourceTx, BigDecimal amount);

    @Named("transferSourceWallet")
    default TransferRsDTO.WalletSource transferSourceWallet(Transaction sourceTx) {
        val sourceWallet = sourceTx.getWallet();
        return new TransferRsDTO.WalletSource(sourceWallet.getUuid(), sourceWallet.getBalance());
    }

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

    @Named("destinationAccountType")
    default AccountType destinationAccountType(AccountInfoRsDTO dto) {
        return switch (dto.bankAccountType()) {
            case CVU -> AccountType.BANK_CVU;
            case CBU -> AccountType.BANK_CBU;
        };
    }
}
