package com.recargapay.wallet.usecase;

import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.helper.JsonHelper;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.DepositArrivedDTO;
import com.recargapay.wallet.mapper.TransactionMapper;
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
    private final JsonHelper jsonHelper;
    private WalletService walletService;
    private TransactionMapper txMapper;
    private TransactionService transactionService;
    private final NotificationService notificationService;

    @Transactional
    public boolean processDeposit(DepositArrivedDTO dto) {
        val wallet = walletService.fetchActiveWalletBy(dto.destinationCvu(), dto.destinationAlias())
                .orElseThrow(() -> new WalletException("There is no active wallet for the given destinationAlias or CVU", INTERNAL_SERVER_ERROR, false));
        val transaction = txMapper.fromDeposit(dto, wallet);
        if (isDuplicateTx(transaction)) {
            log.warn("TRANSACTION IS DUPLICATE, skipping transaction {}", jsonHelper.serialize(transaction));
            return false;
        }
        transactionService.save(transaction);
        updateBalance(wallet, transaction);
        val email = wallet.getUser().getEmail();
        val id = notificationService.sendDepositComplete(email);
        log.info("DEPOSIT_COMPLETE notification id:{} sending to:{}", id, email);
        return true;
    }

    private void updateBalance(Wallet wallet, Transaction transaction) {
        if (TransactionStatus.COMPLETED.equals(transaction.getStatus())) {
            var newBalance = wallet.getBalance().add(transaction.getAmount());
            wallet.setBalance(newBalance);
            walletService.save(wallet);
        }
    }

    private boolean isDuplicateTx(Transaction transaction) {
        return transactionService.existsTx(transaction);
    }
}
