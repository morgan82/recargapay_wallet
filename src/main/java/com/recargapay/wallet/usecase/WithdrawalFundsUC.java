package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.WithdrawalFundsRqDTO;
import com.recargapay.wallet.controller.data.WithdrawalRsDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import com.recargapay.wallet.integration.http.corebanking.data.Status;
import com.recargapay.wallet.integration.notification.NotificationService;
import com.recargapay.wallet.integration.redis.RedisLockManager;
import com.recargapay.wallet.integration.sqs.listener.corebanking.data.WithdrawalCompleteDTO;
import com.recargapay.wallet.persistence.entity.AccountType;
import com.recargapay.wallet.persistence.entity.Transaction;
import com.recargapay.wallet.persistence.entity.TransactionStatus;
import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.service.TransactionService;
import com.recargapay.wallet.persistence.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@AllArgsConstructor
@Validated
@Slf4j
public class WithdrawalFundsUC {
    private final RedisLockManager redisLockManager;
    private final CoreBankingClient coreBankingClient;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    @Transactional
    public WithdrawalRsDTO createWithdrawal(@Valid WithdrawalFundsRqDTO dto, @NotNull UUID walletId) {
        return redisLockManager.runWithDebitFundsLock(walletId, () -> {
            val sourceWallet = walletService.fetchActiveWalletByOrThrow(walletId);
            if (!canDebit(dto, sourceWallet)) {
                throw new WalletException("Insufficient funds", HttpStatus.CONFLICT, true);
            }
            val destinationAccount = getDestinationAccount(dto);
            if (destinationAccount.isRpUser() && destinationAccount.cvbu().equals(sourceWallet.getCvu())) {
                throw new WalletException("Source and destination wallets must be different", HttpStatus.CONFLICT, true);
            }
            val txId = UUID.randomUUID();
            callWdrlCoreBanking(dto, sourceWallet, txId, destinationAccount);
            val txWithdrawal = transactionService.saveWithdrawal(sourceWallet, destinationAccount, txId, dto.withdrawalAmount());
            updateBalanceAndSave(txWithdrawal, dto.withdrawalAmount());
            val withdrawalDTO = mapToDto(txWithdrawal, dto.withdrawalAmount());
            sendPendingNotification(sourceWallet.getUser().getEmail());
            return withdrawalDTO;
        });
    }

    @Transactional
    public void completeWithdrawal(WithdrawalCompleteDTO dto) {
        val wdrlTx = transactionService.findByIdOrThrow(dto.txId());
        transactionService.updateWithdrawal(wdrlTx, dto);
        if (TransactionStatus.FAILED == dto.transactionStatus()) {
            completeWdrlFail(wdrlTx, dto.amount());
        } else {
            completeWdrlComplete(wdrlTx);
        }
    }

    private void completeWdrlComplete(Transaction wdrlTx) {
        val email = wdrlTx.getWallet().getUser().getEmail();
        val id = notificationService.sendWithdrawalCompleted(email);
        log.info("WITHDRAWAL_COMPLETE notification id:{} sending to:{}", id, email);
    }

    private void completeWdrlFail(Transaction wdrlTx, BigDecimal withdrawalAmount) {
        val wallet = wdrlTx.getWallet();
        wallet.setBalance(wallet.getBalance().add(withdrawalAmount));
        walletService.save(wallet);
        val email = wallet.getUser().getEmail();
        val id = notificationService.notifyWithdrawalFail(email);
        log.info("WITHDRAWAL_FAIL notification id:{} sending to:{}", id, email);
    }

    //private methods
    private void callWdrlCoreBanking(WithdrawalFundsRqDTO dto, Wallet sourceWallet, UUID txId, AccountInfoRsDTO destinationAccount) {
        val withdrawalRs = coreBankingClient.withdrawal(sourceWallet.getCvu(), txId, destinationAccount.cvbu(), dto.withdrawalAmount());
        if (Objects.nonNull(withdrawalRs.error())) {
            throw new WalletException("Error code:%s, details:%s".formatted(withdrawalRs.error().code(), withdrawalRs.error().details()), HttpStatus.CONFLICT, true);
        }
    }

    private WithdrawalRsDTO mapToDto(Transaction txWithdrawal, BigDecimal amount) {
        val destinationBankAccountType = AccountType.BANK_CVU.equals(txWithdrawal.getDestinationAccountType()) ? "CVU" : "CBU";
        val destinationAccountId = txWithdrawal.getDestinationAccountId();
        val status = txWithdrawal.getStatus();
        val walletSource = txWithdrawal.getWallet();
        val walletSourceDTO = new WithdrawalRsDTO.WalletSource(walletSource.getUuid(), walletSource.getBalance());
        return new WithdrawalRsDTO(destinationBankAccountType, destinationAccountId, amount, status, walletSourceDTO);
    }

    private void updateBalanceAndSave(Transaction txWithdrawal, BigDecimal wdrlAmount) {
        val wallet = txWithdrawal.getWallet();
        wallet.setBalance(wallet.getBalance().subtract(wdrlAmount));
        walletService.save(wallet);
    }

    private AccountInfoRsDTO getDestinationAccount(WithdrawalFundsRqDTO dto) {
        AccountInfoRsDTO accountInfo;
        if (isAllBlank(dto.destinationAlias(), dto.destinationCVU_CBU())) {
            throw new WalletException("Either alias, CVU or CBU must be provided", HttpStatus.CONFLICT, true);
        }
        if (isNotBlank(dto.destinationAlias())) {
            accountInfo = coreBankingClient.infoByAlias(dto.destinationAlias());
            if (Status.OK.equals(accountInfo.status())) {
                return accountInfo;
            } else {
                throw new WalletException("Invalid Alias", HttpStatus.CONFLICT, true);
            }
        } else {
            accountInfo = coreBankingClient.infoByCvbu(dto.destinationCVU_CBU());
            if (Status.OK.equals(accountInfo.status())) {
                return accountInfo;
            } else {
                throw new WalletException("Invalid CVU/CBU", HttpStatus.CONFLICT, true);
            }
        }
    }

    private static boolean canDebit(WithdrawalFundsRqDTO dto, Wallet source) {
        return source.getBalance().compareTo(dto.withdrawalAmount()) >= 0;
    }

    private void sendPendingNotification(String email) {
        val id = notificationService.notifyWithdrawalPending(email);
        log.info("WITHDRAWAL_PENDING notification id:{} sending to:{}", id, email);
    }
}
