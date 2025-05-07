package com.recargapay.wallet.integration.sqs.listener.corebanking.data;

public record CoreBankingError(
        String details,
        String code) {

}