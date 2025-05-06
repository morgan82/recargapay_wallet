package com.recargapay.wallet.integration.http.corebanking.data;

public record AccountInfoRsDTO(
        String cvbu,
        String alias,
        BankAccountType bankAccountType,
        Status status,
        Boolean isRpUser
) {
    public enum BankAccountType {
        CVU,
        CBU
    }
}
