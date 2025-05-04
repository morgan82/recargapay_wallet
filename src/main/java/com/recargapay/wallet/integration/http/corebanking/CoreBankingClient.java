package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CoreBankingRsDTO;
import com.recargapay.wallet.usecase.data.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

public interface CoreBankingClient {
    CoreBankingRsDTO createCvu(UUID userId, String alias, CurrencyType currency);

    CoreBankingRsDTO deposit(DepositSimulatedDTO rq);
    void withdrawal(UUID walletId, BigDecimal amount);

}
