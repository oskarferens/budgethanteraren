package org.example.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import org.example.exception.InvalidTransactionException;
import org.example.model.Transaction;
import org.example.model.TransactionType;
import org.example.service.BudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The console user interface. Reads input with Scanner and calls BudgetService.
 * All input is read as whole lines, so wrong input never crashes the program.
 */
public class ConsoleMenu {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMenu.class);

    private final BudgetService service;
    private final Scanner scanner;

    public ConsoleMenu(BudgetService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Välj: ");
            if (choice == null) {
                break; // no more input (e.g. Ctrl+D)
            }
            try {
                switch (choice.toLowerCase()) {
                    case "1" -> addTransaction();
                    case "e" -> running = false;
                    default -> System.out.println("Ogiltigt val: \"" + choice + "\". Försök igen.");
                }
            } catch (InvalidTransactionException | IllegalArgumentException | IllegalStateException e) {
                log.warn("Invalid input: {}", e.getMessage());
                System.out.println("Fel: " + e.getMessage());
            }
        }
        System.out.println("Hejdå!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("-----Budgethanteraren-----");
        System.out.println("1. Lägg till transaktion");
        System.out.println("e. Avsluta");
    }

    //Menu actions
    private void addTransaction() {
        TransactionType type = readType();
        LocalDate date = readDate();
        String category = readCategory();
        BigDecimal amount = readAmount();

        Transaction transaction = new Transaction(date, category, amount, type);
        service.addTransaction(transaction);
        System.out.println("Transaktionen lades till: " + format(transaction));
    }

    //Formatting
    private String format(Transaction t) {
        return t.date() + "  " + t.type().getDisplayName() + "  " + t.category()
                + "  " + formatAmount(t.signedAmount()) + " kr";
    }

    private String formatAmount(BigDecimal amount) {
        return (amount.signum() > 0 ? "+" : "") + amount.toPlainString();
    }

    //Safe input
    /** Returns the trimmed line, or null if there is no more input. */
    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    private String readRequired(String prompt) {
        String input = readLine(prompt);
        if (input == null) {
            throw new IllegalStateException("Inmatningen avbröts.");
        }
        return input;
    }

    private TransactionType readType() {
        while (true) {
            String input = readRequired("Typ (1 = Inkomst, 2 = Utgift): ");
            switch (input) {
                case "1" -> { return TransactionType.INCOME; }
                case "2" -> { return TransactionType.EXPENSE; }
                default -> System.out.println("Skriv 1 eller 2.");
            }
        }
    }

    private LocalDate readDate() {
        while (true) {
            String input = readRequired("Datum (ÅÅÅÅ-MM-DD, tom rad = idag): ");
            if (input.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("\"" + input + "\" är inget giltigt datum. Exempel: 2026-03-15");
            }
        }
    }

    private String readCategory() {
        while (true) {
            String input = readRequired("Kategori: ");
            if (input.isEmpty()) {
                System.out.println("Kategorin får inte vara tom.");
            } else if (input.contains(";")) {
                // Semicolon is the separator in the CSV file.
                System.out.println("Kategorin får inte innehålla semikolon (;).");
            } else {
                return input;
            }
        }
    }

    private BigDecimal readAmount() {
        while (true) {
            String input = readRequired("Belopp (t.ex. 450,50): ");
            try {
                // Accept both Swedish decimal comma and decimal point.
                return new BigDecimal(input.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("\"" + input + "\" är inget giltigt belopp.");
            }
        }
    }
}