package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRsDTO;
import com.recargapay.wallet.usecase.data.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

public interface CoreBankingClient {
    CreateCvuRsDTO createCvu(UUID userId, String alias, CurrencyType currency);

    void withdrawal(UUID walletId, BigDecimal amount);
}
