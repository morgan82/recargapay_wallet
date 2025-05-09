package com.recargapay.wallet.integration.sqs.listener.corebanking;

import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.CvuCreatedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.WithdrawalCompleteDTO;
import com.recargapay.wallet.usecase.CreateWalletUC;
import com.recargapay.wallet.usecase.DepositFundsUC;
import com.recargapay.wallet.usecase.WithdrawalFundsUC;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreBankingListener {
    private final CreateWalletUC createWalletUC;
    private final DepositFundsUC depositFundsUC;
    private final WithdrawalFundsUC withdrawalFundsUC;
    private final JsonHelper jsonHelper;

    @SqsListener(value = "${sqs.cvu-created.name}")
    public void handleCvuCreatedEvent(CvuCreatedDTO payload, MessageHeaders headers, Acknowledgement ack) {
        log.info("WALLET_CVU_CREATED event payload:{}", jsonHelper.serialize(payload));
        createWalletUC.completeWalletCreation(payload);//handle business error, sending to dead letter queue
        ack.acknowledge();
    }

    @SqsListener(value = "${sqs.deposit-arrive.name}")
    public void handleDepositArrivedEvent(DepositArrivedDTO payload, MessageHeaders headers, Acknowledgement ack) {
        log.info("DEPOSIT_ARRIVED event payload:{}", jsonHelper.serialize(payload));
        depositFundsUC.processDeposit(payload);//handle business error, sending to dead letter queue
        ack.acknowledge();
    }

    @SqsListener(value = "${sqs.withdrawal-complete.name}")
    public void handleWithdrawalCompletedEvent(WithdrawalCompleteDTO payload, MessageHeaders headers, Acknowledgement ack) {
        log.info("WITHDRAWAL_COMPLETED event payload:{}", jsonHelper.serialize(payload));
        withdrawalFundsUC.completeWithdrawal(payload);//handle business error, sending to dead letter queue
        ack.acknowledge();
    }

}
