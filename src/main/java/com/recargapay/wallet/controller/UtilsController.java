package com.recargapay.wallet.controller;

import com.recargapay.wallet.controller.data.DepositSimulatedRqDTO;
import com.recargapay.wallet.exception.WalletException;
import com.recargapay.wallet.integration.http.corebanking.CoreBankingClient;
import com.recargapay.wallet.integration.http.corebanking.data.AccountInfoRsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@RequestMapping("/utils")
@AllArgsConstructor
public class UtilsController {
    private final CoreBankingClient coreBankingClient;

    @PostMapping("/simulate/deposit")
    @Operation(
            summary = "Simulate a deposit (testing only)",
            description = """
                    This endpoint is intended for **testing purposes only**.
                    It simulates a deposit manually initiated by a user.
                    In a real scenario, deposit events should arrive **asynchronously**
                    from the CoreBanking system via the **SQS queue: `sqs-recargapay-local-deposit-arrived`**.
                    """)
    @RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            value = """
                                    {
                                        "amount": 500000,
                                        "destination_alias": "test.1ars.rp",
                                        "source_cbu": "2850590940090418135201",
                                        "source_cvu": null,
                                        "external_tx_id": "00000-00001"
                                    }
                                    """
                    )
            )
    )
    public void depositFunds(@org.springframework.web.bind.annotation.RequestBody DepositSimulatedRqDTO rq) {
        if (isNotBlank(rq.sourceCvu()) == isNotBlank(rq.sourceCbu())) {//XOR
            throw new WalletException("You must provide exactly one of source CVU or source CBU", HttpStatus.BAD_REQUEST, true);
        }

        val rs = coreBankingClient.deposit(rq);
        if (Objects.nonNull(rs.error())) {
            val message = "Error in deposit, code:%s, details:%s".formatted(rs.error().code(), rs.error().details());
            throw new WalletException(message, HttpStatus.CONFLICT, true);
        }
    }

    @GetMapping("/account-by-alias")
    public Map<String, AccountInfoRsDTO> listCvbuByAlias(@RequestParam(value = "only_rp_account", required = false) Boolean onlyRPayAccount) {
        val allAccounts = coreBankingClient.listAccounts();
        if (onlyRPayAccount != null && onlyRPayAccount) {
            return allAccounts.entrySet().stream()
                    .filter(e -> e.getValue().isRpUser())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return allAccounts;
        }

    }

}
