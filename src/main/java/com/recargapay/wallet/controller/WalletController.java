package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.TransferDTO;
import com.recargapay.wallet.controller.data.TransferFundsRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.service.WalletService;
import com.recargapay.wallet.usecase.CreateWalletUC;
import com.recargapay.wallet.usecase.TransferFundsUC;
import com.recargapay.wallet.usecase.data.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@AllArgsConstructor
public class WalletController {
    private final CreateWalletUC createWalletUC;
    private final TransferFundsUC transferFundsUC;
    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDTO createWallet(@Valid @RequestBody CreateWalletRqDTO newWalletDTO) {
        return createWalletUC.createWallet(newWalletDTO);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferDTO transfer(@Valid @RequestBody TransferFundsRqDTO dto) {
        if (dto.sourceWalletId().equals(dto.destinationWalletId())) {
            throw new WalletException("Source and destination wallets must be different", HttpStatus.CONFLICT, true);
        }
        return transferFundsUC.transfer(dto);
    }

    @GetMapping()
    public WalletDTO getWalletByUsernameAndCurrency(
            @Schema(example = "mLopez") @RequestParam("username") String username,
            @Schema(example = "ARS") @RequestParam("currency") CurrencyType currency
    ) {
        val wallet = walletService.fetchActiveWalletByUsernameAndCurrency(username, currency)
                .orElseThrow(() -> new WalletException("Wallet not found", HttpStatus.NOT_FOUND, true));
        return walletMapper.toWalletDTO(wallet);
    }
}
