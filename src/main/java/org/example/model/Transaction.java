package org.example.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One income or expense. The amount is always positive.
 * The type decides if it adds to or subtracts from the balance.
 * BigDecimal is used for money to avoid rounding errors (0.1 + 0.2 != 0.3 with double).
 */
public record Transaction(LocalDate date, String category, BigDecimal amount, TransactionType type) {

    public Transaction {
        if (date == null) {
            throw new IllegalArgumentException("Datum saknas.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Kategori får inte vara tom.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Typ saknas.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Beloppet måste vara större än 0.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Beloppet får ha högst två decimaler.");
        }
        category = category.trim();
        amount = amount.setScale(2, RoundingMode.UNNECESSARY); // 100 -> 100.00
    }

    /** Positive for income, negative for expense. */
    public BigDecimal signedAmount() {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }
}