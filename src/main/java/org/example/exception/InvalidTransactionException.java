package org.example.exception;

/**
 * Thrown when a transaction has invalid data as missing date or a zero amount.
 * Unchecked, because it is caused by wrong input and is handled where the input is read.
 */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}