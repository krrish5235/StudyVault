package com.studyvault.common.error;

import org.springframework.http.HttpStatus;

/** Internal storage failures - never leak provider details to clients. */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
