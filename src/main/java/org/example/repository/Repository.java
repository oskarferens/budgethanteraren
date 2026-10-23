package org.example.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic in memory storage for objects of any type T.
 * The same class can store transactions, categories or anything else.
 */
public class Repository<T> {

    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        Objects.requireNonNull(item, "item must not be null");
        items.add(item);
        log.debug("Added {} (total: {})", item, items.size());
    }

    /** Returns a read only copy, so nobody can change the list from outside. */
    public List<T> findAll() {
        return List.copyOf(items);
    }

    /**
     * Finds all items that match the given condition.
     * example - repo.findWhere(t -> t.type() == TransactionType.EXPENSE)
     * returns all transactions that are expenses.
     */
    public List<T> findWhere(Predicate<T> condition) {
        Objects.requireNonNull(condition, "condition must not be null");
        List<T> result = items.stream()
                .filter(condition)
                .toList();
        log.debug("findWhere matched {} of {} items", result.size(), items.size());
        return result;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}