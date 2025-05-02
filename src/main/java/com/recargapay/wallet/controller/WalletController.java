package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.usecase.CreateWalletUC;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@AllArgsConstructor
public class WalletController {
    private final CreateWalletUC createWalletUC;

    @PostMapping
    public WalletDTO createWallet(@Valid @RequestBody CreateWalletRqDTO newWalletDTO) {
        return createWalletUC.createWallet(newWalletDTO);
    }
}
