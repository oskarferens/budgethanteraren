package org.example.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepositoryTest {

    private Repository<Transaction> repository;

    private final Transaction salary = new Transaction(
            LocalDate.of(2026, 3, 25), "Lön", new BigDecimal("25000"), TransactionType.INCOME);
    private final Transaction rent = new Transaction(
            LocalDate.of(2026, 3, 1), "Hyra", new BigDecimal("8000"), TransactionType.EXPENSE);
    private final Transaction food = new Transaction(
            LocalDate.of(2026, 3, 10), "Mat", new BigDecimal("450.50"), TransactionType.EXPENSE);

    @BeforeEach
    void setUp() {
        repository = new Repository<>();
    }

    @Test
    @DisplayName("A new repository is empty")
    void newRepositoryIsEmpty() {
        // Act
        List<Transaction> all = repository.findAll();

        // Assert
        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("Added items are returned by findAll in the same order")
    void addStoresItemsInOrder() {
        // Arrange
        repository.add(salary);
        repository.add(rent);

        // Act
        List<Transaction> all = repository.findAll();

        // Assert
        assertEquals(2, repository.size());
        assertFalse(repository.isEmpty());
        assertEquals(List.of(salary, rent), all);
    }

    @Test
    @DisplayName("findAll returns a read only list")
    void findAllReturnsReadOnlyList() {
        // Arrange
        repository.add(salary);
        List<Transaction> all = repository.findAll();

        // Act + Assert
        assertThrows(UnsupportedOperationException.class, () -> all.add(rent));
        assertEquals(1, repository.size());
    }

    @Test
    @DisplayName("findAll returns a copy that does not change when the repository changes")
    void findAllReturnsCopy() {
        // Arrange
        repository.add(salary);
        List<Transaction> before = repository.findAll();

        // Act
        repository.add(rent);

        // Assert
        assertEquals(1, before.size());
        assertEquals(2, repository.size());
    }

    @Test
    @DisplayName("Adding null is not allowed")
    void addNullThrows() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertTrue(repository.isEmpty());
    }

    @Test
    @DisplayName("findWhere returns only the items that match the condition")
    void findWhereReturnsMatchingItems() {
        // Arrange
        repository.add(salary);
        repository.add(rent);
        repository.add(food);

        // Act
        List<Transaction> expenses = repository.findWhere(t -> t.type() == TransactionType.EXPENSE);

        // Assert
        assertEquals(List.of(rent, food), expenses);
    }

    @Test
    @DisplayName("findWhere returns an empty list when nothing matches")
    void findWhereWithNoMatchReturnsEmptyList() {
        // Arrange
        repository.add(rent);
        repository.add(food);

        // Act
        List<Transaction> incomes = repository.findWhere(t -> t.type() == TransactionType.INCOME);

        // Assert
        assertTrue(incomes.isEmpty());
    }

    @Test
    @DisplayName("findWhere on an empty repository returns an empty list")
    void findWhereOnEmptyRepositoryReturnsEmptyList() {
        // Act
        List<Transaction> result = repository.findWhere(t -> true);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findWhere with a null condition is not allowed")
    void findWhereWithNullConditionThrows() {
        assertThrows(NullPointerException.class, () -> repository.findWhere(null));
    }

    @Test
    @DisplayName("The repository is generic and works with other types too")
    void worksWithOtherTypes() {
        // Arrange
        Repository<String> categories = new Repository<>();
        categories.add("Mat");
        categories.add("Hyra");
        categories.add("Nöje");

        // Act
        List<String> startingWithM = categories.findWhere(c -> c.startsWith("M"));

        // Assert
        assertEquals(List.of("Mat"), startingWithM);
        assertEquals(3, categories.size());
    }
}