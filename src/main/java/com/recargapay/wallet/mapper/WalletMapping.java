package com.recargapay.wallet.mapper;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.persistence.entity.User;
import com.recargapay.wallet.persistence.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WalletMapping {

    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cvu", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "status", expression = "java(WalletStatus.PENDING)")
    @Mapping(target = "balance", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "uuid", expression = "java(UUID.randomUUID())")
    @Mapping(target = "alias", source = "dto.alias")
    @Mapping(target = "currency", source = "dto.currency")
    Wallet toNewWalletEntity(CreateWalletRqDTO dto, User user);

    @Mapping(target = "id", source = "uuid")
    WalletDTO toWalletDTO(Wallet wallet);
}
