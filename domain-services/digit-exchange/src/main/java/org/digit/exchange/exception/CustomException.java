package org.digit.exchange.exception;

public class CustomException extends RuntimeException {
    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
