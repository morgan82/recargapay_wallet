package com.recargapay.wallet.integration.http.corebanking.data;

import java.util.Objects;

public record AccountInfoRsDTO(
        String cvbu,
        String alias,
        BankAccountType bankAccountType,
        Status status,
        Boolean isRpUser
) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AccountInfoRsDTO that = (AccountInfoRsDTO) o;
        return Objects.equals(cvbu, that.cvbu) && Objects.equals(alias, that.alias) && Objects.equals(isRpUser, that.isRpUser) && bankAccountType == that.bankAccountType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cvbu, alias, bankAccountType, isRpUser);
    }

    public enum BankAccountType {
        CVU,
        CBU
    }
}
