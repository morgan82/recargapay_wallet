package com.recargapay.wallet.integration.sqs.listener.corebanking.data;

import com.recargapay.wallet.persistence.entity.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalCompleteDTO(
        UUID txId,
        String destinationCvbu,
        String destinationAlias,
        BigDecimal amount,
        String externalTxId,
        TransactionStatus transactionStatus,
        CoreBankingError coreBankingError
) {

}
