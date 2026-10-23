package org.example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.example.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BudgetServiceTest {

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(new Repository<>());
    }

    /** Small helper so the tests are easier to read. */
    private Transaction tx(String date, String category, String amount, TransactionType type) {
        return new Transaction(LocalDate.parse(date), category, new BigDecimal(amount), type);
    }

    //Totals and balance
    @Test
    @DisplayName("Empty budget has 0.00 in balance, income and expenses")
    void emptyBudgetHasZeroTotals() {
        // When + Then
        assertEquals(new BigDecimal("0.00"), service.getBalance());
        assertEquals(new BigDecimal("0.00"), service.getTotalIncome());
        assertEquals(new BigDecimal("0.00"), service.getTotalExpenses());
    }

    @Test
    @DisplayName("Totals and balance are calculated from income and expenses")
    void calculatesTotalsAndBalance() {
        // Given
        service.addTransaction(tx("2026-03-25", "Lön", "25000", TransactionType.INCOME));
        service.addTransaction(tx("2026-03-01", "Hyra", "8000", TransactionType.EXPENSE));
        service.addTransaction(tx("2026-03-10", "Mat", "450.50", TransactionType.EXPENSE));

        // When
        BigDecimal income = service.getTotalIncome();
        BigDecimal expenses = service.getTotalExpenses();
        BigDecimal balance = service.getBalance();

        // Then
        assertEquals(new BigDecimal("25000.00"), income);
        assertEquals(new BigDecimal("8450.50"), expenses);
        assertEquals(new BigDecimal("16549.50"), balance);
    }

    @Test
    @DisplayName("Balance can be negative when expenses are bigger than income")
    void balanceCanBeNegative() {
        // Given
        service.addTransaction(tx("2026-03-01", "Extra", "100", TransactionType.INCOME));
        service.addTransaction(tx("2026-03-02", "Mat", "100.01", TransactionType.EXPENSE));

        // When
        BigDecimal balance = service.getBalance();

        // Then
        assertEquals(new BigDecimal("-0.01"), balance);
    }

    @Test
    @DisplayName("Decimal amounts are added exactly (0.10 + 0.20 = 0.30)")
    void decimalAmountsAreExact() {
        // Given
        service.addTransaction(tx("2026-03-01", "Godis", "0.10", TransactionType.EXPENSE));
        service.addTransaction(tx("2026-03-02", "Godis", "0.20", TransactionType.EXPENSE));

        // When
        BigDecimal expenses = service.getTotalExpenses();

        // Then
        assertEquals(new BigDecimal("0.30"), expenses);
    }

    //Sum per category
    @Test
    @DisplayName("Sum per category is empty when there are no transactions")
    void sumPerCategoryIsEmptyWithoutTransactions() {
        assertTrue(service.getSumPerCategory().isEmpty());
    }

    @Test
    @DisplayName("Sum per category groups transactions and sorts categories A-Z")
    void sumPerCategoryGroupsAndSorts() {
        // Given
        service.addTransaction(tx("2026-03-10", "Mat", "450.50", TransactionType.EXPENSE));
        service.addTransaction(tx("2026-03-25", "Lön", "25000", TransactionType.INCOME));
        service.addTransaction(tx("2026-03-17", "Mat", "300", TransactionType.EXPENSE));
        service.addTransaction(tx("2026-03-01", "Hyra", "8000", TransactionType.EXPENSE));

        // When
        Map<String, BigDecimal> sums = service.getSumPerCategory();

        // Then
        assertEquals(List.of("Hyra", "Lön", "Mat"), new ArrayList<>(sums.keySet()));
        assertEquals(new BigDecimal("-8000.00"), sums.get("Hyra"));
        assertEquals(new BigDecimal("25000.00"), sums.get("Lön"));
        assertEquals(new BigDecimal("-750.50"), sums.get("Mat"));
    }

    //Sorting and filtering
    @Test
    @DisplayName("Categories with different upper/lower case are summed together")
    void sumPerCategoryIgnoresCase() {
        // Arrange
        service.addTransaction(tx("2026-03-10", "Mat", "100", TransactionType.EXPENSE));
        service.addTransaction(tx("2026-03-11", "mat", "50", TransactionType.EXPENSE));

        // Act
        Map<String, BigDecimal> sums = service.getSumPerCategory();

        // Assert
        assertEquals(1, sums.size());
        assertEquals(new BigDecimal("-150.00"), sums.get("Mat"));
    }

    @Test
    @DisplayName("Filter by type returns only that type, sorted by date")
    void filterByTypeReturnsOnlyThatType() {
        // Given
        Transaction salary = tx("2026-03-25", "Lön", "25000", TransactionType.INCOME);
        Transaction food = tx("2026-03-10", "Mat", "450.50", TransactionType.EXPENSE);
        Transaction rent = tx("2026-03-01", "Hyra", "8000", TransactionType.EXPENSE);
        service.addTransaction(salary);
        service.addTransaction(food);
        service.addTransaction(rent);

        // When
        List<Transaction> expenses = service.filterByType(TransactionType.EXPENSE);

        // Then
        assertEquals(List.of(rent, food), expenses);
    }

    @Test
    @DisplayName("Date range includes transactions on the first and the last day")
    void dateRangeIncludesBothEndDates() {
        // Given
        Transaction before = tx("2026-02-28", "Mat", "100", TransactionType.EXPENSE);
        Transaction firstDay = tx("2026-03-01", "Hyra", "8000", TransactionType.EXPENSE);
        Transaction lastDay = tx("2026-03-31", "Mat", "200", TransactionType.EXPENSE);
        Transaction after = tx("2026-04-01", "Lön", "25000", TransactionType.INCOME);
        service.addTransaction(before);
        service.addTransaction(firstDay);
        service.addTransaction(lastDay);
        service.addTransaction(after);

        // When
        List<Transaction> march = service.filterByDateRange(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        // Then
        assertEquals(List.of(firstDay, lastDay), march);
    }

    @Test
    @DisplayName("Date range with the same start and end date returns that day only")
    void dateRangeWithSingleDay() {
        // Given
        Transaction food = tx("2026-03-10", "Mat", "450.50", TransactionType.EXPENSE);
        service.addTransaction(food);
        service.addTransaction(tx("2026-03-11", "Mat", "99", TransactionType.EXPENSE));

        // When
        List<Transaction> result = service.filterByDateRange(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10));

        // Then
        assertEquals(List.of(food), result);
    }

    @Test
    @DisplayName("Start date after end date is not allowed")
    void dateRangeWithStartAfterEndThrows() {
        // When + Then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.filterByDateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1)));
        assertEquals("Startdatum kan inte vara efter slutdatum.", e.getMessage());
    }

    @Test
    @DisplayName("Filters on an empty budget return empty lists")
    void filtersOnEmptyBudgetReturnEmptyLists() {
        // When + Then
        assertTrue(service.getAllSortedByDate().isEmpty());
        assertTrue(service.filterByType(TransactionType.INCOME).isEmpty());
        assertTrue(service.filterByDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).isEmpty());
    }

    @Test
    @DisplayName("Service cannot be created without a repository")
    void constructorWithNullRepositoryThrows() {
        assertThrows(NullPointerException.class, () -> new BudgetService(null));
    }
}