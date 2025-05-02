package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.persistence.entity.Wallet;
import com.recargapay.wallet.persistence.repository.WalletRepository;
import com.recargapay.wallet.usecase.data.CurrencyType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WalletService {
    private WalletRepository walletRepository;

    public Optional<Wallet> fetchWalletByUserIfExist(UUID userUuid) {
        return walletRepository.getByUser_Uuid(userUuid);
    }

    public boolean walletExistByUserAndCurrency(UUID userUuid, CurrencyType currency) {
        long count = walletRepository.countWalletByUser_UuidAndCurrency(userUuid, currency.name());
        return count > 0;
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }
}
