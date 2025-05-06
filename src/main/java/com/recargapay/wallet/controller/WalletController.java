package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.TransferFundsRqDTO;
import com.recargapay.wallet.controller.data.TransferRsDTO;
import com.recargapay.wallet.controller.data.WalletRsDTO;
import com.recargapay.wallet.controller.data.WithdrawalFundsRqDTO;
import com.recargapay.wallet.controller.data.WithdrawalRsDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.service.WalletService;
import com.recargapay.wallet.usecase.CreateWalletUC;
import com.recargapay.wallet.usecase.TransferFundsUC;
import com.recargapay.wallet.usecase.WithdrawalFundsUC;
import com.recargapay.wallet.usecase.data.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/wallet")
@AllArgsConstructor
public class WalletController {
    private final CreateWalletUC createWalletUC;
    private final TransferFundsUC transferFundsUC;
    private final WithdrawalFundsUC withdrawalFundsUC;
    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletRsDTO createWallet(@Valid @RequestBody CreateWalletRqDTO newWalletDTO) {
        return createWalletUC.createWallet(newWalletDTO);
    }

    @PostMapping("/{walletId}/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferRsDTO transfer(@Valid @RequestBody TransferFundsRqDTO dto, @NotNull @PathVariable("walletId") UUID walletId) {
        if (walletId.equals(dto.destinationWalletId())) {
            throw new WalletException("Source and destination wallets must be different", HttpStatus.CONFLICT, true);
        }
        return transferFundsUC.transfer(dto, walletId);
    }

    @PostMapping("/{walletId}/withdrawal")
    @ResponseStatus(HttpStatus.CREATED)
    public WithdrawalRsDTO withdrawal(@Valid @RequestBody WithdrawalFundsRqDTO dto, @NotNull @PathVariable("walletId") UUID walletId) {
        if (StringUtils.isAllBlank(dto.destinationAlias(), dto.destinationCVU_CBU())) {
            throw new WalletException("Either alias, CVU or CBU must be provided", HttpStatus.CONFLICT, true);
        }
        return withdrawalFundsUC.createWithdrawal(dto, walletId);
    }

    @GetMapping()
    public WalletRsDTO getWalletByUsernameAndCurrency(
            @Schema(example = "mLopez") @RequestParam("username") String username,
            @Schema(example = "ARS") @RequestParam("currency") CurrencyType currency
    ) {
        val wallet = walletService.fetchActiveWalletByUsernameAndCurrency(username, currency)
                .orElseThrow(() -> new WalletException("Wallet not found", HttpStatus.NOT_FOUND, true));
        return walletMapper.toWalletDTO(wallet);
    }
}
