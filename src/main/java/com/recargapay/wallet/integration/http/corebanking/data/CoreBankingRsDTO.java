package com.recargapay.wallet.integration.http.corebanking.data;

public record CoreBankingRsDTO(
        Status status,
        Error error
) {
    public record Error(
            String code,
            String details
    ) {

    }

    public enum Status {
        OK, ERROR
    }
}
