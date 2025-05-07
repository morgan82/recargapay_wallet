package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.persistence.entity.User;
import com.recargapay.wallet.persistence.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUserByUuid(UUID uuid) {
        return userRepository.getByUuid(uuid)
                .orElseThrow(() -> new WalletException("User do not exist", HttpStatus.BAD_REQUEST, true));
    }
}
