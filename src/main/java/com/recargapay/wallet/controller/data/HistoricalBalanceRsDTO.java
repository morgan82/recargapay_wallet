package com.recargapay.wallet.controller.data;

import com.recargapay.wallet.persistence.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HistoricalBalanceRsDTO(
        UUID walletId,
        BigDecimal balance,
        String currency,
        LocalDateTime at,
        WalletStatus status
) {
}
