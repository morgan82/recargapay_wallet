package com.recargapay.wallet.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected final ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        val detail = ProblemDetail.forStatus(status);
        val errorList = ex.getBindingResult().getAllErrors().stream().map(e -> {
            String fieldName = e instanceof FieldError ?
                    ((FieldError) e).getField() :
                    e.getObjectName();
            String message = e.getDefaultMessage();
            return fieldName + "-> " + message;
        }).collect(Collectors.joining(", "));
        detail.setTitle("Validation Error");
        detail.setType(URI.create(getFullURL(request)));
        detail.setDetail(errorList);
        return ResponseEntity.status(status).body(detail);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        val detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Resource not found");
        detail.setType(URI.create(getFullURL(request)));
        detail.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

    @ExceptionHandler(WalletException.class)
    public ResponseEntity<ProblemDetail> handleWalletException(WalletException ex, HttpServletRequest request) {
        val detail = ProblemDetail.forStatus(ex.getStatusCode());
        val title = ex.isClientError() ? "Client Error" : "Server Error";
        detail.setTitle(title);
        detail.setType(URI.create(getFullURL(request)));
        detail.setDetail(ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(Exception ex, HttpServletRequest request) {
        HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        val detail = ProblemDetail.forStatus(internalServerError);

        detail.setTitle("Unexpected Error");
        detail.setType(URI.create(getFullURL(request)));
        detail.setDetail(ex.getMessage());
        return ResponseEntity.status(internalServerError).body(detail);
    }

    private String getFullURL(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            val httpRequest = servletWebRequest.getNativeRequest(HttpServletRequest.class);
            return nonNull(httpRequest) ? getFullURL(httpRequest) : "";
        } else {
            return "";
        }
    }

    private String getFullURL(HttpServletRequest request) {
        val requestURL = request.getRequestURL();
        val queryString = request.getQueryString();
        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

}
