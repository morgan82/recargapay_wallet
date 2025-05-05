package com.recargapay.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recargapay.wallet.controller.data.CreateWalletRqDTO;
import com.recargapay.wallet.controller.data.WalletDTO;
import com.recargapay.wallet.mapper.WalletMapper;
import com.recargapay.wallet.persistence.entity.WalletStatus;
import com.recargapay.wallet.persistence.service.WalletService;
import com.recargapay.wallet.usecase.CreateWalletUC;
import com.recargapay.wallet.usecase.TransferFundsUC;
import com.recargapay.wallet.usecase.data.CurrencyType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {WalletController.class})
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferFundsUC transferFundsUC;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private WalletMapper walletMapper;

    @MockitoBean
    private CreateWalletUC createWalletUC;

    @Test
    void shouldCreateWalletSuccessfully_WhenGivenValidRequest() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletDTO walletDTO = new WalletDTO(walletId, "1234567890", "user.wallet.test", BigDecimal.ZERO, "USD", WalletStatus.ACTIVE);

        Mockito.when(createWalletUC.createWallet(any(CreateWalletRqDTO.class))).thenReturn(walletDTO);

        CreateWalletRqDTO createWalletRqDTO = new CreateWalletRqDTO(
                UUID.randomUUID(),
                CurrencyType.USD,
                "user.wallet.test"
        );

        mockMvc.perform(post("/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRqDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.cvu").value("1234567890"))
                .andExpect(jsonPath("$.alias").value("user.wallet.test"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnBadRequest_WhenGivenInvalidRequest() throws Exception {
        CreateWalletRqDTO invalidRequest = new CreateWalletRqDTO(
                null,
                null,
                "invalid-destinationAlias"
        );

        mockMvc.perform(post("/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldHandleInternalServerError_WhenCreateWalletFails() throws Exception {
        Mockito.when(createWalletUC.createWallet(any(CreateWalletRqDTO.class))).thenThrow(new RuntimeException("Runtime error"));

        CreateWalletRqDTO createWalletRqDTO = new CreateWalletRqDTO(
                UUID.randomUUID(),
                CurrencyType.USD,
                "user.wallet.test"
        );

        mockMvc.perform(post("/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletRqDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Runtime error"));
    }
}