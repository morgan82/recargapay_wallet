package com.recargapay.wallet.controller.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositSimulatedRqDTO(
        @NotBlank
        @Schema(example = "test.1ars.rp")
        String destinationAlias,
        @Positive
        @NotNull
        BigDecimal amount,
        String sourceCvu,
        String sourceCbu,
        @NotBlank
        String externalTxId
) {
}
