package com.recargapay.wallet.controller.data;

import com.recargapay.wallet.usecase.data.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateWalletRqDTO(
        @NotNull
        @Schema(example = "5e62c324-ab6b-496a-a310-efc53c30fe39")
        UUID userId,
        @NotNull
        @Schema(example = "ARS")
        CurrencyType currency,
        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){2}$",
                message = "Alias must be in the format word.word.word with alphanumeric parts"
        )
        @Schema(example = "test.2ars.rp")
        String alias
) {
}
