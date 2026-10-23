package org.example.storage;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.exception.FileFormatException;
import org.example.model.Transaction;
import org.example.model.TransactionType;

/**
 * Reads and writes transactions as CSV lines,
 * example (...)2026-03-10;Mat;450.50;EXPENSE.
 * Semicolon is used as separator because comma is the decimal sign in Swedish.
 */
public class TransactionFileStorage {

    static final String HEADER = "date;category;amount;type";
    private static final String SEPARATOR = ";";
    private static final int FIELD_COUNT = 4;

    /** Turns one transaction into one CSV line. */
    String toCsvLine(Transaction transaction) {
        return transaction.date()
                + SEPARATOR + transaction.category()
                + SEPARATOR + transaction.amount().toPlainString()
                + SEPARATOR + transaction.type().name();
    }

    /** Turns one CSV line into a transaction, or throws if the line is broken. */
    Transaction parseLine(String line, int lineNumber) throws FileFormatException {
        String[] parts = line.split(SEPARATOR, -1);
        if (parts.length != FIELD_COUNT) {
            throw new FileFormatException(
                    "förväntade " + FIELD_COUNT + " fält men hittade " + parts.length, lineNumber, null);
        }
        try {
            LocalDate date = LocalDate.parse(parts[0].trim());
            String category = parts[1].trim();
            BigDecimal amount = new BigDecimal(parts[2].trim());
            TransactionType type = TransactionType.valueOf(parts[3].trim());
            return new Transaction(date, category, amount, type);
        } catch (RuntimeException e) {
            // DateTimeParseException, NumberFormatException, IllegalArgumentException
            // and InvalidTransactionException are all RuntimeExceptions.
            throw new FileFormatException("ogiltig rad \"" + line + "\"", lineNumber, e);
        }
    }
}