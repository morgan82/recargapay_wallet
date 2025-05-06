package com.recargapay.wallet.usecase;

import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.service.TransactionService;
import com.recargapay.wallet.persistence.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
@AllArgsConstructor
@Slf4j
public class DepositFundsUC {
    private WalletService walletService;
    private TransactionService transactionService;
    private final NotificationService notificationService;

    @Transactional
    public void processDeposit(DepositArrivedDTO dto) {
        val wallet = walletService.fetchActiveWalletBy(dto.destinationCvu(), dto.destinationAlias())
                .orElseThrow(() -> new WalletException("There is no active wallet for the given destinationAlias or CVU", INTERNAL_SERVER_ERROR, false));
        val txOptional = transactionService.saveDeposit(dto, wallet);
        if (txOptional.isEmpty()) {//skipped, duplicate tx
            return;
        }
        updateBalanceAndSave(wallet, txOptional.get());
        sendNotification(wallet);
    }

    //private methods
    private void sendNotification(Wallet wallet) {
        val email = wallet.getUser().getEmail();
        val id = notificationService.notifyDepositCompleted(email);
        log.info("DEPOSIT_COMPLETE notification id:{} sending to:{}", id, email);
    }

    private void updateBalanceAndSave(Wallet wallet, Transaction transaction) {
        if (TransactionStatus.COMPLETED.equals(transaction.getStatus())) {
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletService.save(wallet);
        }
    }

}
