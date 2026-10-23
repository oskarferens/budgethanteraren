package org.example.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

import org.example.exception.InvalidTransactionException;

/**
 * One income or expense. The amount is always positive -
 * the type decides if it adds to or subtracts from the balance.
 * BigDecimal is used for money to avoid rounding errors (0.1 + 0.2 != 0.3 with double).
 */
public record Transaction(LocalDate date, String category, BigDecimal amount, TransactionType type) {

    public Transaction {
        if (date == null) {
            throw new InvalidTransactionException("Datum saknas.");
        }
        if (category == null || category.isBlank()) {
            throw new InvalidTransactionException("Kategori får inte vara tom.");
        }
        if (type == null) {
            throw new InvalidTransactionException("Typ saknas.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("Beloppet måste vara större än 0.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidTransactionException("Beloppet får ha högst två decimaler.");
        }
        category = normalizeCategory(category);
        amount = amount.setScale(2, RoundingMode.UNNECESSARY); // 100 -> 100.00
    }

    /**
     * "mat", "MAT" and " Mat " all become "Mat", so the same category
     * is always grouped together in the summary.
     */
    private static String normalizeCategory(String category) {
        String trimmed = category.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Positive for income, negative for expense. */
    public BigDecimal signedAmount() {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }
}