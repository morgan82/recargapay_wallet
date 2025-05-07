package com.recargapay.wallet.controller.data;

import com.recargapay.wallet.persistence.entity.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletRsDTO(
        UUID id,
        String cvu,
        String alias,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {
}
