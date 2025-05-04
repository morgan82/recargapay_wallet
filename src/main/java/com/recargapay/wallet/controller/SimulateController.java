package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.DepositSimulatedDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@RequestMapping("/simulate")
@AllArgsConstructor
public class SimulateController {
    private final CoreBankingClient coreBankingClient;

    @PostMapping("/deposit")
    @RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            value = """
                                    {
                                        "destination_cvu": null,
                                        "amount": 500000,
                                        "destination_alias": "test.1ars.rp",
                                        "source_cbu": "2850590940090418135201",
                                        "external_tx_id": "00000-00001"
                                    }
                                    """
                    )
            )
    )
    public void depositFunds(@org.springframework.web.bind.annotation.RequestBody DepositSimulatedDTO rq) {
        if (isNotBlank(rq.sourceCvu()) == isNotBlank(rq.sourceCbu())) {//XOR
            throw new WalletException("You must provide exactly one of source CVU or source CBU", HttpStatus.BAD_REQUEST, true);
        }

        val rs = coreBankingClient.deposit(rq);
        if (Objects.nonNull(rs.error())) {
            val message = "Error in deposit, code:%s, details:%s".formatted(rs.error().code(), rs.error().details());
            throw new WalletException(message, HttpStatus.CONFLICT, true);
        }
    }


}
