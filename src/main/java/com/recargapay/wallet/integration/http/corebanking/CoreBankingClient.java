package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedRqDTO;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.DefaultRsDTO;
import com.recargapay.wallet.usecase.data.CurrencyType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface CoreBankingClient {
    DefaultRsDTO createCvu(UUID userId, String alias, CurrencyType currency);

    DefaultRsDTO deposit(DepositSimulatedRqDTO rq);

    DefaultRsDTO withdrawal(String sourceCvu, UUID txId, String destinationCvbu, BigDecimal amount);

    AccountInfoRsDTO infoByAlias(String alias);

    AccountInfoRsDTO infoByCvbu(String cvuCbu);

    Map<String, AccountInfoRsDTO> listAccounts();
}
