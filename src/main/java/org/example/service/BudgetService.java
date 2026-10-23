package org.example.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.example.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic for the budget. Add transactions and calculating totals.
 * Knows nothing about the console or files, which makes it easy to test.
 */
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    /** Zero with two decimals, so an empty budget is shown as 0.00. */
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final Repository<Transaction> repository;

    public BudgetService(Repository<Transaction> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public void addTransaction(Transaction transaction) {
        repository.add(transaction);
        log.debug("Transaction stored: {}", transaction);
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
        BigDecimal balance = repository.findAll().stream()
                .map(Transaction::signedAmount)
                .reduce(ZERO, BigDecimal::add);
        log.debug("Balance calculated: {}", balance);
        return balance;
    }

    /**
     * Sum per category -Hyra = -8000.00, Lön = 25000.00, Mat = -450.50-.
     * Income is positive and expenses are negative. TreeMap keeps the categories sorted A-Z.
     */
    public Map<String, BigDecimal> getSumPerCategory() {
        Map<String, BigDecimal> sums = repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        TreeMap::new,
                        Collectors.reducing(ZERO, Transaction::signedAmount, BigDecimal::add)));
        log.debug("Sum per category calculated for {} categories", sums.size());
        return sums;
    }

    /** All transactions sorted by date, oldest first. */
    public List<Transaction> getAllSortedByDate() {
        return sortByDate(repository.findAll());
    }

    /** Only income or only expenses, sorted by date. */
    public List<Transaction> filterByType(TransactionType type) {
        Objects.requireNonNull(type, "type must not be null");
        log.debug("Filtering by type {}", type);
        return sortByDate(repository.findWhere(t -> t.type() == type));
    }

    /** Transactions between two dates, both dates included, sorted by date. */
    public List<Transaction> filterByDateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Startdatum kan inte vara efter slutdatum.");
        }
        log.debug("Filtering by date range {} - {}", from, to);
        return sortByDate(repository.findWhere(
                t -> !t.date().isBefore(from) && !t.date().isAfter(to)));
    }

    private List<Transaction> sortByDate(List<Transaction> transactions) {
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::date))
                .toList();
    }

    private BigDecimal sumOf(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::amount)
                .reduce(ZERO, BigDecimal::add);
    }
}