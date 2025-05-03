package com.recargapay.wallet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Getter
public class WalletException extends HttpStatusCodeException {
    final boolean clientError;

    public WalletException(String message, HttpStatus status, boolean clientError) {
        super(status, message);
        this.clientError = clientError;
    }
}
