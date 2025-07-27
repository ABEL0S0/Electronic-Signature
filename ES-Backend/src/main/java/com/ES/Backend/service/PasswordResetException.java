package com.ES.Backend.service;

/**
 * Exception thrown when password reset code is invalid or expired.
 */
public class PasswordResetException extends RuntimeException {
    public PasswordResetException(String message) {
        super(message);
    }
}
