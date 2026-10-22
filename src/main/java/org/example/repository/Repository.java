package org.example.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic in memory storage for objects of any type T.
 * The same class can store transactions, categories or anything else.
 */
public class Repository<T> {

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        Objects.requireNonNull(item, "item must not be null");
        items.add(item);
    }

    /** Returns a read only copy, so nobody can change the list from outside. */
    public List<T> findAll() {
        return List.copyOf(items);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}