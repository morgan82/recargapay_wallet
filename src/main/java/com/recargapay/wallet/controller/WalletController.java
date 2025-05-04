package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.service.WalletService;
import com.recargapay.wallet.usecase.CreateWalletUC;
import com.recargapay.wallet.usecase.data.CurrencyType;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@AllArgsConstructor
public class WalletController {
    private final CreateWalletUC createWalletUC;
    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @PostMapping
    public WalletDTO createWallet(@Valid @RequestBody CreateWalletRqDTO newWalletDTO) {
        return createWalletUC.createWallet(newWalletDTO);
    }

    @GetMapping()
    public WalletDTO getWalletByUsernameAndCurrency(
            @RequestParam("username") String username,
            @RequestParam("currency") CurrencyType currency
    ) {
        val wallet = walletService.fetchActiveWalletByUsernameAndCurrency(username, currency)
                .orElseThrow(() -> new WalletException("Wallet not found", HttpStatus.NOT_FOUND, true));
        return walletMapper.toWalletDTO(wallet);
    }
}
