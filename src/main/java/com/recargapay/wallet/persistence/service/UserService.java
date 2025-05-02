package com.recargapay.wallet.persistence.service;

import com.recargapay.wallet.persistence.entity.User;
import com.recargapay.wallet.persistence.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public User getUserByUuid(UUID uuid) {
        return userRepository.getByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User do not exist"));
    }
}
