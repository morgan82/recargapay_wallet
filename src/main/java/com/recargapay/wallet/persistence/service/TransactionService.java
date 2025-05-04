package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionService {
    private TransactionRepository transactionRepository;

    public Transaction save(Transaction tx) {
        return transactionRepository.save(tx);
    }

    public boolean existsTx(Transaction tx) {
        val destinationAId = tx.getDestinationAccountId();
        val sourceAId = tx.getSourceAccountId();
        val externalTxId = tx.getExternalTxId();
        val walletUuid = tx.getWallet().getUuid();
        return transactionRepository.existTx(walletUuid, externalTxId, destinationAId, sourceAId) > 0;
    }
}
