package com.recargapay.wallet.usecase;

import com.recargapay.wallet.controller.data.HistoricalBalanceRsDTO;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.service.TransactionService;
import com.recargapay.wallet.persistence.service.WalletService;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
@Validated
public class HistoricalBalanceUC {
    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final TransactionService transactionService;

    public HistoricalBalanceRsDTO balance(@NotNull UUID walletId, @NotNull LocalDateTime at) {
        val wallet = walletService.fetchByUuidOrThrow(walletId);
        val balanceAt = transactionService.sumAmountBy(walletId, at);
        return walletMapper.toHistoricalBalanceDTO(wallet, at, balanceAt);
    }

}
