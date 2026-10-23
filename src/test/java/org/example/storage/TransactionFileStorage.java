package org.example.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.example.exception.FileFormatException;
import org.example.exception.InvalidTransactionException;
import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransactionFileStorageTest {

    /** JUnit creates a new empty folder for every test and deletes it afterwards. */
    @TempDir
    Path tempDir;

    private TransactionFileStorage storage;
    private Path file;

    private final Transaction food = new Transaction(
            LocalDate.of(2026, 3, 10), "Mat", new BigDecimal("450.50"), TransactionType.EXPENSE);
    private final Transaction salary = new Transaction(
            LocalDate.of(2026, 3, 25), "Lön", new BigDecimal("25000"), TransactionType.INCOME);

    @BeforeEach
    void setUp() {
        storage = new TransactionFileStorage();
        file = tempDir.resolve("transactions.csv");
    }

    @Test
    @DisplayName("Loading a missing file creates a new file with only a header")
    void loadCreatesFileWhenMissing() throws IOException {
        // Given
        Path missing = tempDir.resolve("data").resolve("budget.csv");

        // When
        List<Transaction> result = storage.load(missing);

        // Then
        assertTrue(result.isEmpty());
        assertTrue(Files.exists(missing));
        assertEquals(List.of(TransactionFileStorage.HEADER), Files.readAllLines(missing));
    }

    @Test
    @DisplayName("Loading a completely empty file returns an empty list")
    void loadEmptyFileReturnsEmptyList() throws IOException {
        // Given
        Files.createFile(file);

        // When
        List<Transaction> result = storage.load(file);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Broken lines are skipped and the valid lines are still loaded")
    void loadSkipsBrokenLines() throws IOException {
        // Given
        Files.write(file, List.of(
                TransactionFileStorage.HEADER,
                "2026-03-10;Mat;450.50;EXPENSE",   // ok
                "2026-13-01;Mat;100;EXPENSE",      // month 13
                "2026-03-11;Mat;abc;EXPENSE",      // not a number
                "2026-03-12;Mat;0;EXPENSE",        // zero amount
                "2026-03-13;Mat;100;GIFT",         // unknown type
                "2026-03-14;Mat;100",              // missing field
                "",                                // blank line
                "2026-03-25;Lön;25000.00;INCOME"   // ok
        ), StandardCharsets.UTF_8);

        // When
        List<Transaction> result = storage.load(file);

        // Then
        assertEquals(List.of(food, salary), result);
    }

    @Test
    @DisplayName("Save writes a header and one line per transaction")
    void saveWritesHeaderAndLines() throws IOException {
        // When
        storage.save(file, List.of(food, salary));

        // Then
        assertEquals(List.of(
                TransactionFileStorage.HEADER,
                "2026-03-10;Mat;450.50;EXPENSE",
                "2026-03-25;Lön;25000.00;INCOME"
        ), Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Saving an empty list writes only the header")
    void saveEmptyListWritesOnlyHeader() throws IOException {
        // When
        storage.save(file, List.of());

        // Then
        assertEquals(List.of(TransactionFileStorage.HEADER), Files.readAllLines(file));
    }

    @Test
    @DisplayName("Save replaces the old content of the file")
    void saveOverwritesOldContent() throws IOException {
        // Given
        storage.save(file, List.of(food, salary));

        // When
        storage.save(file, List.of(salary));

        // Then
        assertEquals(List.of(salary), storage.load(file));
    }

    @Test
    @DisplayName("Transactions are the same after save and load - round trip")
    void saveThenLoadGivesSameTransactions() throws IOException {
        // Given
        List<Transaction> original = List.of(food, salary);

        // When
        storage.save(file, original);
        List<Transaction> loaded = storage.load(file);

        // Then
        assertEquals(original, loaded);
    }

    @Test
    @DisplayName("A transaction is written as date;category;amount;type")
    void toCsvLineFormat() {
        assertEquals("2026-03-10;Mat;450.50;EXPENSE", storage.toCsvLine(food));
    }

    @Test
    @DisplayName("A valid line is parsed into a transaction")
    void parseValidLine() throws FileFormatException {
        // When
        Transaction result = storage.parseLine(" 2026-03-10 ; Mat ; 450.50 ; EXPENSE ", 2);

        // Then
        assertEquals(food, result);
    }

    @Test
    @DisplayName("Wrong number of fields gives FileFormatException with the line number")
    void parseLineWithWrongFieldCountThrows() {
        // When
        FileFormatException e = assertThrows(FileFormatException.class,
                () -> storage.parseLine("2026-03-10;Mat;450.50", 7));

        // Then
        assertEquals(7, e.getLineNumber());
        assertTrue(e.getMessage().startsWith("Rad 7:"));
    }

    @Test
    @DisplayName("Invalid date keeps the original error as cause")
    void parseLineWithInvalidDateThrows() {
        // When
        FileFormatException e = assertThrows(FileFormatException.class,
                () -> storage.parseLine("10/03/2026;Mat;450.50;EXPENSE", 3));

        // Then
        assertInstanceOf(DateTimeParseException.class, e.getCause());
    }

    @Test
    @DisplayName("Invalid amount keeps the original error as cause")
    void parseLineWithInvalidAmountThrows() {
        // When
        FileFormatException e = assertThrows(FileFormatException.class,
                () -> storage.parseLine("2026-03-10;Mat;450,50;EXPENSE", 4));

        // Then
        assertInstanceOf(NumberFormatException.class, e.getCause());
    }

    @Test
    @DisplayName("A line with amount 0 fails on the Transaction validation")
    void parseLineWithZeroAmountThrows() {
        // When
        FileFormatException e = assertThrows(FileFormatException.class,
                () -> storage.parseLine("2026-03-10;Mat;0;EXPENSE", 5));

        // Then
        assertInstanceOf(InvalidTransactionException.class, e.getCause());
    }
}