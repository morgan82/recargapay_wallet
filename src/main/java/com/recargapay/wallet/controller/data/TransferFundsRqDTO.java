package com.recargapay.wallet.controller.data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferFundsRqDTO(
        @NotNull UUID destinationWalletId,
        @NotNull @DecimalMin(value = "1.00") BigDecimal transferAmount
) {
}
