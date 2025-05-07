package com.recargapay.wallet.integration.http.corebanking;

import com.recargapay.wallet.controller.data.DepositSimulatedRqDTO;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.CreateCvuRqDTO;
import com.recargapay.wallet.integration.http.corebanking.data.DefaultRsDTO;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class CoreBankingClientImpl implements CoreBankingClient {
    private final WebClient coreBankingWebClient;

    @Override
    public DefaultRsDTO createCvu(UUID userId, String alias, CurrencyType currency) {
        return Objects.requireNonNull(coreBankingWebClient.post()
                .uri("/cvu")
                .bodyValue(new CreateCvuRqDTO(userId, alias, currency))
                .retrieve()
                .toEntity(DefaultRsDTO.class)
                .block()).getBody();
    }

    @Override
    public DefaultRsDTO deposit(DepositSimulatedRqDTO rq) {
        return null;
    }

    @Override
    public DefaultRsDTO withdrawal(String sourceCvu, UUID txId, String destinationCvbu, BigDecimal amount) {
        return null;
    }


    @Override
    public AccountInfoRsDTO infoByAlias(String alias) {
        return null;
    }

    @Override
    public AccountInfoRsDTO infoByCvbu(String cvuCbu) {
        return null;
    }

    @Override
    public Map<String, AccountInfoRsDTO> listAccounts() {
        return Map.of();
    }
}
