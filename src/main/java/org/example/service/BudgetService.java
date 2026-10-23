package org.example.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.example.repository.Repository;

/**
 * Business logic for the budget. Add transactions and calculating totals.
 * Knows nothing about the console or files, which makes it easy to test.
 */
public class BudgetService {

    /** Zero with two decimals, so an empty budget is shown as 0.00. */
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final Repository<Transaction> repository;

    public BudgetService(Repository<Transaction> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public void addTransaction(Transaction transaction) {
        repository.add(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    public BigDecimal getTotalIncome() {
        return sumOf(repository.findWhere(t -> t.type() == TransactionType.INCOME));
    }

    public BigDecimal getTotalExpenses() {
        return sumOf(repository.findWhere(t -> t.type() == TransactionType.EXPENSE));
    }

    /** Income minus expenses. */
    public BigDecimal getBalance() {
        return repository.findAll().stream()
                .map(Transaction::signedAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumOf(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::amount)
                .reduce(ZERO, BigDecimal::add);
    }
}