package com.recargapay.wallet.mapper;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.HistoricalBalanceRsDTO;
import com.recargapay.wallet.controller.data.WalletRsDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.persistence.entity.User;
import com.recargapay.wallet.persistence.entity.Wallet;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.recargapay.wallet.persistence.entity.WalletStatus.ACTIVE;
import static com.recargapay.wallet.persistence.entity.WalletStatus.PENDING;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WalletMapper {

    @Mapping(target = "extraInfo", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cvu", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "alias", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "status", expression = "java(WalletStatus.PENDING)")
    @Mapping(target = "balance", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "currency", source = "dto.currency")
    Wallet toNewWalletEntity(CreateWalletRqDTO dto, User user);

    @Mapping(target = "id", source = "uuid")
    WalletRsDTO toWalletDTO(Wallet wallet);

    @Mapping(target = "walletId", source = "wallet.uuid")
    @Mapping(target = "balance", source = "historicalBalance")
    @Mapping(target = "at", source = "at")
    @Mapping(target = "currency", source = "wallet.currency")
    @Mapping(target = "status", source = "wallet.status")
    HistoricalBalanceRsDTO toHistoricalBalanceDTO(Wallet wallet, LocalDateTime at, BigDecimal historicalBalance);

    default void updateWallet(Wallet wallet, CvuCreatedDTO dto) {
        val status = dto.status();
        if (ACTIVE.equals(status) || PENDING.equals(status)) {
            wallet.setCvu(dto.cvu());
            wallet.setAlias(dto.alias());
        } else {
            val extraInfo = "ERROR, Code:%s, Detail:%s".formatted(dto.coreBankingError().code(), dto.coreBankingError().details());
            wallet.setExtraInfo(StringUtils.truncate(extraInfo, 300));
        }
        wallet.setStatus(status);
    }

}
