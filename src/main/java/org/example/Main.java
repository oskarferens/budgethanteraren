package org.example;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.service.BudgetService;
import org.example.storage.TransactionFileStorage;
import org.example.ui.ConsoleMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Path DATA_FILE = Path.of("transactions.csv");

    static void main() {
        Repository<Transaction> repository = new Repository<>();
        BudgetService service = new BudgetService(repository);
        TransactionFileStorage storage = new TransactionFileStorage();

        boolean loaded = loadTransactions(storage, service);

        ConsoleMenu menu = new ConsoleMenu(service, storage, DATA_FILE, new Scanner(System.in));
        menu.run();

        if (loaded) {
            menu.saveToFile();
        } else {
            // Do not overwrite a file we could not read - the user would lose the old data.
            System.out.println("Automatisk sparning hoppades över för att det gick inte att läsas filen vid start.");
        }
        System.out.println("Hejdå!");
    }

    /** Loads saved transactions. Returns false if the file could not be read. */
    private static boolean loadTransactions(TransactionFileStorage storage, BudgetService service) {
        try {
            storage.load(DATA_FILE).forEach(service::addTransaction);
            return true;
        } catch (IOException e) {
            log.error("Could not read {}", DATA_FILE, e);
            System.out.println("Kunde inte läsa filen " + DATA_FILE + ". Programmet startar med en tom budget.");
            return false;
        }
    }
}