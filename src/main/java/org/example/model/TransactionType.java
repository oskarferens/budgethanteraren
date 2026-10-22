package org.example.model;

public enum TransactionType {
    INCOME("Inkomst"),
    EXPENSE("Utgift");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}