package com.recargapay.wallet.integration.http.corebanking.data;

import com.recargapay.wallet.usecase.data.CurrencyType;

import java.util.UUID;

public record CreateCvuRqDTO(
        UUID userId,
        String alias,
        CurrencyType currency
) {
}
