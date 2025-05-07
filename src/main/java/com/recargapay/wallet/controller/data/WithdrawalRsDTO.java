package com.recargapay.wallet.controller.data;

import com.recargapay.wallet.persistence.entity.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRsDTO(
        String destinationBankAccountType,
        String destinationBankAccountId,
        BigDecimal amount,
        TransactionStatus txStatus,
        WalletSource sourceWallet
) {
    public record WalletSource(
            UUID sourceWalletId,
            BigDecimal balance
    ) {
    }
}
