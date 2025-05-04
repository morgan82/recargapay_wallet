package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CoreBankingRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRqDTO;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class CoreBankingClientImpl implements CoreBankingClient {
    private final WebClient coreBankingWebClient;

    @Override
    public CoreBankingRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        return Objects.requireNonNull(coreBankingWebClient.post()
                .uri("/cvu")
                .bodyValue(new CreateCvuRqDTO(userId, alias, currency))
                .retrieve()
                .toEntity(CoreBankingRsDTO.class)
                .block()).getBody();
    }

    @Override
    public CoreBankingRsDTO deposit(DepositSimulatedDTO rq) {
        return null;
    }

    @Override
    public void withdrawal(UUID walletId, BigDecimal amount) {

    }
}
