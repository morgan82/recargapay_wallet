package com.recargapay.wallet.integration.sqs.listener.corebanking.data;

import java.math.BigDecimal;

public record DepositArrivedDTO(
        String destinationAlias,
        String destinationCvu,
        BigDecimal amount,
        String sourceCvu,
        String sourceCbu,
        String externalTxId
) {

}
