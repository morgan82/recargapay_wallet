package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRqDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRsDTO;
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
    public CreateCvuRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        return Objects.requireNonNull(coreBankingWebClient.post()
                .uri("/cvu")
                .bodyValue(new CreateCvuRqDTO(userId, alias, currency))
                .retrieve()
                .toEntity(CreateCvuRsDTO.class)
                .block()).getBody();
    }

    @Override
    public void withdrawal(UUID walletId, BigDecimal amount) {

    }
}
