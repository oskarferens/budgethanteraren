package org.example.exception;

/**
 * Thrown when a line in the transaction file cannot be read like a broken date or amount.
 * Checked, because a broken file is something the caller must decide how to handle.
 */
public class FileFormatException extends Exception {

    private final int lineNumber;

    public FileFormatException(String message, int lineNumber, Throwable cause) {
        super("Rad " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}