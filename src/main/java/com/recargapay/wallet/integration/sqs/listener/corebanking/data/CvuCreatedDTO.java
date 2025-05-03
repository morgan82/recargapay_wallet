package com.recargapay.wallet.integration.sqs.listener.corebanking.data;

import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.usecase.data.CurrencyType;

import java.util.UUID;

public record CvuCreatedDTO(
        String alias,
        String cvu,
        String cbu,
        CurrencyType currency,
        WalletStatus status,
        Error error,
        UUID userId
) {
    public record Error(
            String details,
            String code) {

    }
}
