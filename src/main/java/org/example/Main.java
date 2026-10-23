package org.example;

import java.util.Scanner;

import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.service.BudgetService;
import org.example.ui.ConsoleMenu;

public class Main {

    static void main() {
        Repository<Transaction> repository = new Repository<>();
        BudgetService service = new BudgetService(repository);
        ConsoleMenu menu = new ConsoleMenu(service, new Scanner(System.in));
        menu.run();
    }
}