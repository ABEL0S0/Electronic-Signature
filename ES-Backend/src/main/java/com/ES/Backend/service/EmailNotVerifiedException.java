package com.ES.Backend.service;

/**
 * Exception thrown when user tries to authenticate without verifying their email.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
