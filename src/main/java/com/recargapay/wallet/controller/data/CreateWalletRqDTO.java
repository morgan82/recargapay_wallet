package com.recargapay.wallet.controller.data;

import com.recargapay.wallet.usecase.data.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateWalletRqDTO(
        @NotNull
        UUID userId,
        @NotNull
        CurrencyType currency,
        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){2}$",
                message = "Alias must be in the format word.word.word with alphanumeric parts"
        )
        String alias
) {
}
