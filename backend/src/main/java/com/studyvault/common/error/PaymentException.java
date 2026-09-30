package com.studyvault.common.error;

import org.springframework.http.HttpStatus;

/** Payment failures are surfaced as 402/400-safe generic messages. */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
