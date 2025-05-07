package com.recargapay.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${api.core-banking.url}")
    private String coreBankingURL;

    @Bean
    public WebClient coreBankingWebClient() {
        return WebClient.builder()
                .baseUrl(coreBankingURL)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
