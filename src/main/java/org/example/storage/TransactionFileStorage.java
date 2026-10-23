package org.example.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.example.exception.FileFormatException;
import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes transactions as CSV lines,
 * example (...)2026-03-10;Mat;450.50;EXPENSE.
 * Semicolon is used as separator because comma is the decimal sign in Swedish.
 */
public class TransactionFileStorage {

    private static final Logger log = LoggerFactory.getLogger(TransactionFileStorage.class);

    static final String HEADER = "date;category;amount;type";
    private static final String SEPARATOR = ";";
    private static final int FIELD_COUNT = 4;

    /**
     * Reads all transactions from the file.
     * If the file does not exist, a new empty file is created.
     * Broken lines are skipped and logged, so one bad line does not stop the program.
     */
    public List<Transaction> load(Path file) throws IOException {
        if (Files.notExists(file)) {
            log.info("File {} not found, creating a new empty file", file);
            createEmptyFile(file);
            return new ArrayList<>();
        }

        log.debug("Reading transactions from {}", file.toAbsolutePath());
        List<Transaction> transactions = new ArrayList<>();
        int skipped = 0;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || (lineNumber == 1 && line.trim().equals(HEADER))) {
                    continue;
                }
                try {
                    Transaction transaction = parseLine(line, lineNumber);
                    transactions.add(transaction);
                    log.debug("Line {} parsed: {}", lineNumber, transaction);
                } catch (FileFormatException e) {
                    skipped++;
                    log.warn("Skipping broken line in {}: {}", file, e.getMessage());
                }
            }
        }

        log.info("Loaded {} transactions from {} ({} broken lines skipped)",
                transactions.size(), file, skipped);
        return transactions;
    }

    /**
     * Writes all transactions to the file. The old content is replaced.
     */
    public void save(Path file, List<Transaction> transactions) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        log.debug("Writing {} transactions to {}", transactions.size(), file.toAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Transaction transaction : transactions) {
                writer.write(toCsvLine(transaction));
                writer.newLine();
            }
        }

        log.info("Saved {} transactions to {}", transactions.size(), file);
    }

    private void createEmptyFile(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
    }

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