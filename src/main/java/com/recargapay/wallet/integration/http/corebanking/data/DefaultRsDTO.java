package com.recargapay.wallet.integration.http.corebanking.data;

public record DefaultRsDTO(
        Status status,
        Error error
) {
    public record Error(
            String code,
            String details
    ) {

    }

}
