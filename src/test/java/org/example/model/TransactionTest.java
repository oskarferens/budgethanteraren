package org.example.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.exception.InvalidTransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 15);

    @Test
    @DisplayName("Income has a positive signed amount")
    void incomeHasPositiveSignedAmount() {
        // Given
        Transaction salary = new Transaction(DATE, "Lön", new BigDecimal("25000"), TransactionType.INCOME);

        // When
        BigDecimal signed = salary.signedAmount();

        // Then
        assertEquals(new BigDecimal("25000.00"), signed);
    }

    @Test
    @DisplayName("Expense has a negative signed amount")
    void expenseHasNegativeSignedAmount() {
        // Given
        Transaction food = new Transaction(DATE, "Mat", new BigDecimal("450.50"), TransactionType.EXPENSE);

        // When
        BigDecimal signed = food.signedAmount();

        // Then
        assertEquals(new BigDecimal("-450.50"), signed);
    }

    @Test
    @DisplayName("Amount is always stored with two decimals")
    void amountIsNormalizedToTwoDecimals() {
        // Given + When
        Transaction a = new Transaction(DATE, "Mat", new BigDecimal("100"), TransactionType.EXPENSE);
        Transaction b = new Transaction(DATE, "Mat", new BigDecimal("10.500"), TransactionType.EXPENSE);

        // Then
        assertEquals(new BigDecimal("100.00"), a.amount());
        assertEquals(new BigDecimal("10.50"), b.amount());
    }

    @Test
    @DisplayName("Whitespace around the category is removed")
    void categoryIsTrimmed() {
        // Given + When
        Transaction t = new Transaction(DATE, "  Hyra  ", new BigDecimal("8000"), TransactionType.EXPENSE);

        // Then
        assertEquals("Hyra", t.category());
    }

    @Test
    @DisplayName("Smallest positive amount (0.01) is accepted")
    void smallestPositiveAmountIsAccepted() {
        // Given + When
        Transaction t = new Transaction(DATE, "Övrigt", new BigDecimal("0.01"), TransactionType.EXPENSE);

        // Then
        assertEquals(new BigDecimal("0.01"), t.amount());
    }

    @Test
    @DisplayName("More than two decimals is not allowed")
    void threeDecimalsThrows() {
        // Given + When
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", new BigDecimal("0.001"), TransactionType.EXPENSE));
        assertEquals("Beloppet får ha högst två decimaler.", e.getMessage());
    }

    @Test
    @DisplayName("Amount 0 is not allowed")
    void zeroAmountThrows() {
        // Given + When
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", BigDecimal.ZERO, TransactionType.EXPENSE));
        assertEquals("Beloppet måste vara större än 0.", e.getMessage());
    }

    @Test
    @DisplayName("Negative amount is not allowed. The type decides the sign")
    void negativeAmountThrows() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", new BigDecimal("-100"), TransactionType.EXPENSE));
    }

    @Test
    @DisplayName("Missing amount is not allowed")
    void nullAmountThrows() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", null, TransactionType.EXPENSE));
    }

    @Test
    @DisplayName("Blank category is not allowed")
    void blankCategoryThrows() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "   ", new BigDecimal("100"), TransactionType.EXPENSE));
    }

    @Test
    @DisplayName("Missing date is not allowed")
    void nullDateThrows() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(null, "Mat", new BigDecimal("100"), TransactionType.EXPENSE));
    }

    @Test
    @DisplayName("Missing type is not allowed")
    void nullTypeThrows() {
        assertThrows(InvalidTransactionException.class,
                () -> new Transaction(DATE, "Mat", new BigDecimal("100"), null));
    }
}