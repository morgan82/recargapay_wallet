package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.persistence.repository.WalletRepository;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WalletService {
    private WalletRepository walletRepository;

    public Optional<Wallet> fetchWalletByUserIfExist(UUID userUuid, CurrencyType currency, WalletStatus status) {
        return walletRepository.getByUser_UuidAndCurrencyAndStatus(userUuid, currency.name(), status);
    }

    public Optional<Wallet> fetchActiveWalletBy(String cvu, String alias) {
        return walletRepository.getBy(WalletStatus.ACTIVE, cvu, alias);
    }

    public boolean walletExistByUserAndCurrency(UUID userUuid, CurrencyType currency) {
        val validStatus = List.of(WalletStatus.ACTIVE, WalletStatus.PENDING);

        long count = walletRepository.countWalletBy(userUuid, currency.name(), validStatus);
        return count > 0;
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }
}
