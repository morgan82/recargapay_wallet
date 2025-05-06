package com.recargapay.wallet.controller.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record WithdrawalFundsRqDTO(
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){2}$",
                message = "Alias must be in the format word.word.word with alphanumeric parts"
        )
        @Schema(example = "test.2ars.rp")
        String destinationAlias,
        String destinationCVU_CBU,
        @NotNull @DecimalMin(value = "1.00") BigDecimal withdrawalAmount
) {
}
