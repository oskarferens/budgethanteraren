# Budgethanteraren

Lab 2 in Java. A console application where you can register income and expenses, put them in categories, see a summary of your economy and save everything to a CSV file.

## How to run

Requires JDK 26 and Maven.

```
mvn test
mvn compile
java -cp target/classes org.example.Main
```

Or run `Main` in IntelliJ. The data is saved in `transactions.csv` and the log in `budget.log` (both in the project folder, both created automatically).

## Menu

```
1. Lägg till transaktion
2. Visa alla transaktioner
3. Visa saldo och sammanställning per kategori
4. Filtrera transaktioner
5. Spara till fil
e. Avsluta
```

Transactions are loaded from the file at start and saved automatically when the program ends.

## Structure

- `model` - `TransactionType` (enum: INCOME/EXPENSE) and `Transaction` (record: date, category, amount, type)
- `exception` - `InvalidTransactionException` (unchecked) and `FileFormatException` (checked)
- `repository` - `Repository<T>`, my own generic class with `add`, `findAll` and `findWhere(Predicate<T>)`
- `service` - `BudgetService` with balance, totals, sum per category, filtering and sorting (Stream API)
- `storage` - `TransactionFileStorage`, reads and writes the CSV file
- `ui` - `ConsoleMenu`, the menu and safe input with Scanner
- `Main` - starts the program, loads and saves the file

### Some design choices

- **BigDecimal for money.** `double` cannot store 0.1 exactly, so 0.1 + 0.2 becomes 0.30000000000000004. All amounts are stored with two decimals.
- **The amount is always positive.** The type (income/expense) decides the sign. A negative amount, zero or more than two decimals throws `InvalidTransactionException`.
- **CSV with semicolon** (`2026-03-10;Mat;450.50;EXPENSE`), because comma is the decimal sign in Swedish. The menu does not allow `;` in a category.
- **Broken lines in the file** are skipped with a WARN in the log, the rest of the file is still loaded. A missing file is created. If the file cannot be read at all, the program starts with an empty budget and does not overwrite the old file when it exits.
- `FileFormatException` is checked because the caller must decide what to do with a broken file. `InvalidTransactionException` is unchecked because it comes from wrong input and is handled where the input is read.

## Logging

SLF4J with Logback. The console only shows ERROR so the menu stays readable. Everything from DEBUG goes to `budget.log`. Tests use their own `logback-test.xml` (WARN, no file).

| Level | Used for | Example |
|-------|----------|---------|
| DEBUG | details for the developer | menu choice, every parsed line, calculated balance |
| INFO | normal important events | program started/stopped, file loaded/saved, user added a transaction |
| WARN | something is wrong but the program continues | invalid input, broken line in the file, auto-save skipped |
| ERROR | an operation failed | the file could not be read or written |

## Tests

JUnit 5, 50 tests, all written with Arrange-Act-Assert.

- `TransactionTest` - validation, zero/negative/too many decimals, normalized category
- `RepositoryTest` - empty repository, order, read-only copy, `findWhere`, null, works with other types than Transaction
- `BudgetServiceTest` - totals, negative balance, 0.10 + 0.20 = 0.30, sum per category, sorting, date range including both end dates, wrong date order
- `TransactionFileStorageTest` - uses `@TempDir`: missing file, empty file, broken lines, save/load round trip, line number and cause in `FileFormatException`

## Bug found during development: categories with different case

**How I found it.** When I tested the program with unusual input I added an expense in the category `mat` and another one in `Mat`. In the summary (option 3) they were shown as two different categories. The log showed the same thing:

```
INFO  ConsoleMenu - User added transaction: Transaction[date=2026-10-23, category=mat, amount=100.00, type=EXPENSE]
INFO  ConsoleMenu - User added transaction: Transaction[date=2026-10-23, category=Mat, amount=50.00, type=EXPENSE]
DEBUG BudgetService - Sum per category calculated for 3 categories
```

There were 3 categories (`abc`, `mat`, `Mat`) but it should have been 2.

**Test.** I wrote the test `sumPerCategoryIgnoresCase` in `BudgetServiceTest` that adds `Mat` 100 and `mat` 50 and expects one category with -150.00. It failed with `expected: <1> but was: <2>`. I committed the failing test first on the branch `fix/category-case-sensitivity`.

**Debugging.** I put a breakpoint in `BudgetService.getSumPerCategory()` and ran the test with Debug. In the Variables window the map `sums` had two keys, `Mat` and `mat`. I also checked `sums.keySet()` with Evaluate Expression. Then I put a breakpoint in the `Transaction` constructor on the line where the category is trimmed and stepped over it. The category `mat` was still `mat` after `trim()`, so nothing made the case the same. `Collectors.groupingBy(Transaction::category)` compares the strings with `equals`, and `"Mat".equals("mat")` is false, so it made two groups.

**Fix.** The `Transaction` constructor now normalizes the category: first letter upper case, the rest lower case (`mat`, `MAT` and ` mAt ` all become `Mat`). I fixed it in the record and not only in the summary, so the category is the same everywhere: in the list, in the filters and in the file. After the fix the test was green, and I added `categoryCaseIsNormalized` in `TransactionTest`.

## Reflection: generics and Stream API

**Generics.** `Repository<T>` is written once and works for any type. The tests use it both with `Transaction` and with `String`. Without generics I would need a `TransactionRepository` with the same code for every new type, or a repository with `Object` where I have to cast everything and the compiler cannot help me. With `Repository<Transaction>` the compiler checks that only transactions are added, and `findAll()` gives back a `List<Transaction>` without any casts.

`findWhere(Predicate<T>)` made the repository much more reusable. The repository does not know anything about income, dates or categories, it only gets a condition as a lambda. The same method is used for filtering by type (`t -> t.type() == type`), by date range and for the income/expense totals. Without it I would need one method for every kind of filter.

**Stream API.** The summary per category is one expression:

```java
repository.findAll().stream()
        .collect(Collectors.groupingBy(
                Transaction::category,
                TreeMap::new,
                Collectors.reducing(ZERO, Transaction::signedAmount, BigDecimal::add)));
```

Without streams I would need a loop, a map, a check if the key already exists, adding to the old value and putting it back. The stream version says *what* I want (group by category, sum the amounts) instead of *how* to do it step by step. It is the same with the balance (`map` + `reduce`) and the sorting (`sorted(Comparator.comparing(Transaction::date))`). The code is shorter, every method does one thing, and it was easier to test because each calculation is a small method.

One thing I learned is that `summingDouble` does not work with `BigDecimal`, so I used `reducing(ZERO, ..., BigDecimal::add)` instead. Another one is that `groupingBy` is only as good as the keys it gets, which is exactly what caused the bug above.

## Git workflow

Every feature was built on its own branch and merged to `master` with a pull request (merge commit).