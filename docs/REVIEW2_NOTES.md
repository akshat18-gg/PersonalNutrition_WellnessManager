# Review 2 – Viva Notes

Topics: **Packages**, **Interfaces**, **Java Library**, **Exception Handling**.

For each topic you get: what it is, where it is in our code, why we used it, a short script to say out loud, and 5 likely questions with answers.

Tip for the viva: open the file named in the "Where" column and point at the method while you talk.

---

## 1. Packages

**What it is:** A package is a folder that groups related classes and gives them a shared name, like `dao.impl.FoodDAOImpl`.

### Where it is in our code

| Package | What is inside | Its one job |
|---|---|---|
| `model` | `User`, `Food`, `Meal`, `MealItem`, `NutritionGoal`, `WaterIntake`, `DailySummary`, `FoodSuggestion` | Plain data. One object = one row (or one result). No SQL, no Swing. |
| `dao` | `UserDAO`, `FoodDAO`, `MealDAO`, `GoalDAO`, `WaterDAO` | Interfaces that say WHAT can be done with each table. |
| `dao.impl` | `UserDAOImpl`, `FoodDAOImpl`, `MealDAOImpl`, `GoalDAOImpl`, `WaterDAOImpl` | JDBC code that says HOW. **All SQL in the project is here.** |
| `service` | `AuthService`, `NutritionService`, `GoalCalculator`, `ReportExporter`, `TextReportExporter`, `CsvReportExporter` | Logic and calculations. |
| `exception` | `NutriTrackException` and its four subclasses | Our own error types. |
| `util` | `Validator`, `Session`, `PasswordUtil` | Small helpers used everywhere. |
| `database` | `DBConnection` | Opens the MySQL connection from `config/db.properties`. |
| `gui` | All frames and panels, `Refreshable`, `UIStyles` | Screens only. Calls services and DAOs. **No SQL.** |

The direction of calls is always the same:

```text
gui  --->  service  --->  dao (interface)  --->  dao.impl (JDBC)  --->  MySQL
```

A good example to show: `gui/MealsPanel.java`, method `addMealItem()`. It calls `Validator.parseQuantity(...)` (util) and `mealDao.addItem(...)` (dao). There is no `Connection` or SQL string anywhere in that file.

### Why we used it
- In Review 1, every panel had its own SQL and `user_id = 1` was written in many places. A change to one table meant editing several screens.
- Now each kind of code has one home. If a query is wrong we look only in `dao.impl`. If a screen looks wrong we look only in `gui`.
- Two classes can have similar names without clashing, and `import` lines show clearly what each class depends on.

### Script (say this)
> "We split the project into eight packages, one per layer. The `gui` package only has screens, `service` has the logic, `dao` has interfaces for database work and `dao.impl` has the JDBC code. `model` holds the data classes that travel between the layers. Because of this, all our SQL is in one package, and the screens never touch the database directly."

### Likely questions

**Q1. What is a package and why use one?**
A package is a named group of related classes, stored as a folder. It keeps the project organised, avoids name clashes, and lets us control access.

**Q2. How does a class in one package use a class in another?**
With an `import` statement, for example `import model.Food;` in `FoodDAOImpl`. The class being imported must be `public`.

**Q3. Why is `dao.impl` a separate package from `dao`?**
`dao` holds the interfaces (the contract) and `dao.impl` holds the JDBC classes (one way of fulfilling it). The rest of the app depends on the contract. If we changed the database tomorrow, only `dao.impl` would change.

**Q4. How do you compile a project with packages using plain `javac`?**
We list the source folders and give an output folder with `-d bin`. `javac` creates matching folders inside `bin` (for example `bin/dao/impl/FoodDAOImpl.class`). We run with `java -cp "bin;lib\mysql-connector-j.jar" Main`. See `run.bat` and `run.sh`.

**Q5. `Main.java` has no `package` line. Is that a problem?**
It is in the default package. That is fine for the single starting class, and it keeps the run command short (`java Main`). Every other class is in a named package.

---

## 2. Interfaces

**What it is:** An interface is a list of method signatures without bodies. A class that `implements` it promises to provide those methods.

### Where it is in our code

We wrote **three kinds** of interface, and each one does real work.

| Interface | Implemented by | Where it is USED (the important part) |
|---|---|---|
| `dao.UserDAO`, `FoodDAO`, `MealDAO`, `GoalDAO`, `WaterDAO` | The five `...DAOImpl` classes in `dao.impl` | Fields are declared with the interface type. Example: `MealsPanel` has `private final MealDAO mealDao = new MealDAOImpl();`. Same in `AuthService`, `NutritionService`, `FoodLibraryPanel`, `WaterPanel`, `GoalsPanel`, `ProfilePanel`. |
| `gui.Refreshable` (one method: `refreshData()`) | All 7 screens: `DashboardHomePanel`, `MealsPanel`, `FoodLibraryPanel`, `WaterPanel`, `GoalsPanel`, `ProfilePanel`, `WeeklyReportPanel` | `DashboardFrame.showScreen(String name)`: it gets a `Refreshable` from a map and calls `screen.refreshData()`. One line refreshes any screen. |
| `service.ReportExporter` (`getFormatName()`, `export(...)`) | `TextReportExporter`, `CsvReportExporter` | `DashboardHomePanel.exportReport()`: the user picks a format, and `exporter.export(...)` is the same line for both. |

We also implement interfaces that come **from the Java library**:
- `Comparator<Food>` in `NutritionService.suggestFoods()` to rank foods.
- `DocumentListener` in `FoodLibraryPanel.createSearchBar()` for search-as-you-type.
- `Runnable` in `Main.main()` to start Swing.

### Why we used it
- **DAO interfaces:** the screens and services know only *what* a DAO can do. They do not depend on JDBC. Only the words `new MealDAOImpl()` name the real class.
- **Refreshable:** in Review 1, `DashboardFrame` had to call `loadMealsData()`, `loadFoodData()`, `loadGoalsFromDatabase()`, `loadWaterData()`, a different name for every panel. Now there is one method name for all. Adding the Profile and Weekly Report screens needed no change in `showScreen()`.
- **ReportExporter:** adding a new format later (for example HTML) means writing one new class. The button code does not change.

### Script (say this)
> "We use interfaces in three places. First, each table has a DAO interface, and our screens hold the interface type, so they don't depend on JDBC. Second, every screen implements `Refreshable`, so the dashboard refreshes any screen by calling `refreshData()` through one line of code. Third, `ReportExporter` has a text and a CSV implementation, and the export button calls the same method whichever one the user picks. That is polymorphism."

### Likely questions

**Q1. What is the difference between an interface and an abstract class?**
An interface only says what methods must exist; a class can implement many interfaces. An abstract class can also have fields and finished methods, but a class can extend only one. Our panels already extend `JPanel`, so `Refreshable` had to be an interface.

**Q2. Show me polymorphism in your code.**
`DashboardFrame.showScreen()`: the variable `screen` has type `Refreshable`. At run time it may be a `MealsPanel` or a `WaterPanel`, and Java calls the correct `refreshData()`. Also `DashboardHomePanel.exportReport()` with `ReportExporter`.

**Q3. Why write `MealDAO mealDao = new MealDAOImpl();` instead of `MealDAOImpl mealDao = ...`?**
So the class can only use the methods promised by the interface. The real class is named in exactly one place, so it is easy to replace.

**Q4. Can you create an object of an interface?**
No. `new Refreshable()` does not compile. We create an object of a class that implements it. (An anonymous class, like our `Comparator<Food>`, is a class without a name that implements the interface on the spot.)

**Q5. In `DashboardFrame.addScreen()` you cast the panel to `Refreshable`. Why?**
The parameter is a `JPanel` because it must go into the `CardLayout`. The same object is also a `Refreshable`, but a parameter has only one type, so we cast it once to store it in the `screens` map.

---

## 3. Java Library

**What it is:** Ready-made classes that come with the JDK, so we do not write common things ourselves.

### Where it is in our code

| Library class | File and method | Why we used it there |
|---|---|---|
| `ArrayList` / `List` | Every DAO "find" method, e.g. `FoodDAOImpl.findAll()`; `Meal.items`; `MealsPanel.rowItems` | We do not know in advance how many rows the database will return. A list grows as needed. `rowItems` remembers which `MealItem` is on which table row, so we know what to delete. |
| `LinkedHashMap` / `Map` | `MealDAOImpl.findByDate()` (`mealsById`) | The query returns one row per item. The map (meal id → Meal) lets us find the meal we already created and add the item to it. |
| `LinkedHashMap` | `NutritionService.getCaloriesByMealType()` | Totals per meal type. *Linked* keeps the order we inserted: Breakfast, Lunch, Snack, Dinner. |
| `LinkedHashMap` | `NutritionService.getLastSevenDays()` | Totals per day, oldest to newest. Days with no meals are filled in as zero. |
| `LinkedHashMap` | `DashboardFrame.screens` and `navButtons` | Screen name → screen, and screen name → sidebar button. |
| `HashSet` / `Set` | `NutritionService.getLoggingStreak()` | Fast answer to "was anything logged on this date?" |
| `Comparator` + `Collections.sort` | `NutritionService.suggestFoods()` | Sorts the whole food library by the chosen nutrient, highest first. |
| `Properties` | `DBConnection.loadConfig()` | Reads `key=value` lines from `config/db.properties`. `getProperty(key, default)` gives the fallback. |
| `LocalDate` | `NutritionService` (`LocalDate.now()`, `minusDays`, `plusDays`), `MealsPanel`, all DAOs | "Today", "6 days ago", "the day before" without any manual date maths. |
| `LocalDateTime` | `WaterIntake.recordedAt`, `WaterPanel.saveWater()` | Date **and** time of each water entry. |
| `DateTimeFormatter` | `DashboardHomePanel` (`EEEE, dd MMM yyyy`), `WaterPanel` (`hh:mm a`), `CalorieChartPanel` (`EEE dd`), `TextReportExporter` | Turns a date into readable text. |
| `BufferedWriter`, `FileWriter`, `File` | `TextReportExporter.export()`, `CsvReportExporter.export()` | Writes the report file. `File.mkdirs()` creates the `reports` folder. |
| `FileInputStream` | `DBConnection.loadConfig()` | Reads the config file. |
| `MessageDigest` | `PasswordUtil.hash()` | SHA-256 hash of the password. |
| `StringBuilder` | `PasswordUtil.hash()`, `MealsPanel.showTotals()`, `CsvReportExporter.writeRow()` | Builds text piece by piece inside a loop. |
| `Locale` | `CsvReportExporter.number()` | Forces a dot as decimal point so CSV columns do not break. |
| `java.sql` (`Connection`, `PreparedStatement`, `ResultSet`) | Every class in `dao.impl` | JDBC. `PreparedStatement` with `?` placeholders prevents SQL injection. |
| `Graphics2D`, `BasicStroke`, `FontMetrics` | `CalorieChartPanel.paintComponent()` | Draws the bar chart and the dashed goal line by hand. |

### Why we used it
These classes are tested, fast, and every Java programmer knows them. Each one in the table solves a real need in the app; none is there for show.

### Script (say this)
> "From `java.util` we use `ArrayList` for query results, `LinkedHashMap` for totals per meal type and per day, a `HashSet` for the streak, and a `Comparator` with `Collections.sort` to rank foods. From `java.time` we use `LocalDate` for today and the last seven days. From `java.io` we use `BufferedWriter` to export reports, and `Properties` reads our database config. Passwords are hashed with `MessageDigest` using SHA-256."

### Likely questions

**Q1. Why `ArrayList` and not an array?**
An array has a fixed size. We do not know how many foods or meals the query will return, and `ArrayList` grows automatically.

**Q2. What is the difference between `HashMap` and `LinkedHashMap`? Why did you choose the second?**
Both store key → value. `HashMap` gives no order. `LinkedHashMap` remembers insertion order. We need Breakfast before Lunch, and the 7 days from oldest to newest, so order matters.

**Q3. Explain your `Comparator`.**
In `suggestFoods()`, `compare(first, second)` returns `Double.compare(valueOf(second), valueOf(first))`. Putting `second` first makes the order descending, so the food with the most of the nutrient comes first. `Collections.sort(foods, comparator)` does the sorting.

**Q4. Why `LocalDate` instead of the old `java.util.Date`?**
`LocalDate` is just a date with no time and no time zone, which is exactly what a meal date is. Methods like `minusDays(1)` are clear and it cannot be changed by accident (it is immutable). We only use `java.util.Date` in `MealsPanel` because `JSpinner` requires it, and we convert it straight away in `getSelectedDate()`.

**Q5. What is SHA-256 and why hash the password?**
It is a one-way function: the same text always gives the same 64-character result, but you cannot get the text back from the result. If someone reads our `users` table, they see hashes, not passwords. At login we hash what was typed and compare the two hashes (`PasswordUtil.matches`).
*Follow-up they may ask:* "Is plain SHA-256 enough for a real product?" No. Real systems add a random **salt** per user and use a slow algorithm such as bcrypt. We kept plain SHA-256 because it needs no extra library and matches MySQL's `SHA2()` function in our SQL script.

---

## 4. Exception Handling

**What it is:** A way to report and handle errors without crashing: code that finds a problem *throws* an exception, and code that knows what to do about it *catches* it.

### Our exception classes (`exception` package)

```text
Exception  (Java)
   └── NutriTrackException           our base class, CHECKED
          ├── InvalidInputException      the user typed something wrong
          ├── AuthenticationException    wrong email or password
          ├── DuplicateEntryException    email or food name already exists
          └── DatabaseException          a JDBC call failed (keeps the SQLException as its cause)
```

### Where each concept is in our code

| Concept | File and method | What to point at |
|---|---|---|
| **Custom exception** | `exception/NutriTrackException.java` and the four subclasses | `extends Exception` makes it checked. |
| **`throw`** | `util/Validator.java`, e.g. `parseQuantity()` | `throw new InvalidInputException("Quantity must be greater than 0 g.");` Also `AuthService.login()` and `SignUpFrame.readForm()` (passwords do not match). |
| **`throws`** | Every method in `Validator`; every DAO interface method; `SignUpFrame.readForm()` | `readForm() throws InvalidInputException` passes the problem up to `handleSignUp()`, which shows the dialog. |
| **try-with-resources** | Every DAO method, e.g. `FoodDAOImpl.findAll()` | `try (Connection conn = ...; PreparedStatement stmt = ...; ResultSet rs = ...)`. All three are closed automatically. Also `DBConnection.loadConfig()`, `TextReportExporter.export()`, `Main.checkDatabase()`. |
| **try / catch / finally** | `MealDAOImpl.addItem()` and `deleteItem()` | A transaction: `commit()` in `try`, `rollback()` in `catch`, and the connection is closed in `finally` whether it worked or not. |
| **Multi-catch** | `DashboardHomePanel.exportReport()` | `catch (DatabaseException \| IOException ex)`. Also `FoodLibraryPanel.addFood()` with `InvalidInputException \| DuplicateEntryException`. |
| **Exception with a cause** | Every `catch (SQLException e)` in `dao.impl` | `throw new DatabaseException("Could not load the meals.", e);` The `e` is the cause. |
| **Catch order (specific first)** | `UserDAOImpl.insert()` | `SQLIntegrityConstraintViolationException` is caught before `SQLException`, because it is a subclass. Also the three catch blocks in `LoginFrame.handleLogin()`. |
| **Friendly messages, no raw SQL** | `UIStyles.showError()` | The dialog shows our message. The real SQL error goes to the console with `System.err.println`. |
| **Unchecked exception** | `PasswordUtil.hash()`, `Session.getUserId()` | `IllegalStateException` for things that can only happen through a programming mistake. |

### How one error travels through the layers

Example: MySQL is stopped and the user opens the Meals screen.

1. `MealDAOImpl.findByDate()` — JDBC throws `SQLException`.
2. The `catch` there wraps it: `throw new DatabaseException("Could not load the meals.", e);`
3. `MealsPanel.loadMeals()` catches `DatabaseException` and calls `UIStyles.showError(this, ex)`.
4. The user sees "Could not load the meals." The console shows the real cause for us.

Example: the user types `abc` as a quantity.

1. `Validator.parseQuantity()` → `parseNumber()` catches `NumberFormatException` and throws `InvalidInputException`.
2. `MealsPanel.addMealItem()` catches it and shows a warning dialog. Nothing reaches the database.

### Why we used it
- In Review 1 the dialogs showed text like `MySQL Error: Access denied for user...`. That means nothing to a user and it leaks internal details.
- With our own exceptions, each layer speaks its own language: the DAO knows SQL, the GUI knows only "invalid input", "duplicate", "login failed" or "database problem".
- Checked exceptions make the compiler remind us to handle every database call.

### Script (say this)
> "We made a base exception, `NutriTrackException`, with four subclasses. `Validator` throws `InvalidInputException` for bad input before anything reaches the database. The DAO classes catch `SQLException` and wrap it in a `DatabaseException`, keeping the original as the cause. The GUI catches our exceptions and shows a friendly dialog, so the user never sees raw SQL errors. We use try-with-resources in every DAO to close connections, and try-catch-finally in `MealDAOImpl` for the transaction with rollback."

### Likely questions

**Q1. What is the difference between checked and unchecked exceptions? Which are yours?**
Checked exceptions extend `Exception`; the compiler forces you to catch them or declare `throws`. Unchecked ones extend `RuntimeException`; no such rule. Ours are checked, because things like a failed database call are expected problems that the caller must deal with.

**Q2. What is the difference between `throw` and `throws`?**
`throw` is a statement that actually raises an exception object: `throw new InvalidInputException(...)`. `throws` is part of a method signature and warns callers which checked exceptions may come out of it.

**Q3. Why wrap `SQLException` in `DatabaseException`? Why not just let it pass up?**
Then the GUI would have to import `java.sql` and would depend on JDBC. Wrapping keeps SQL details inside `dao.impl`, gives the user a clean message, and still keeps the original error as the cause for debugging (`ex.getCause()`).

**Q4. What does try-with-resources do, and what is `finally` for?**
Try-with-resources automatically calls `close()` on anything opened in its brackets, even if an exception happens. `finally` is a block that always runs after `try`/`catch`. In `MealDAOImpl.addItem()` we need the connection *inside* the `catch` to roll back, so we open it before the `try` and close it in `finally`.

**Q5. Why do you need a transaction in `addItem()`?**
It does up to two inserts: maybe a new row in `meals`, then a row in `meal_items`. If the second fails we must not leave an empty meal behind. With `setAutoCommit(false)`, either both are saved by `commit()` or both are undone by `rollback()`.

---

## Quick reference: one file to open for each mark

| Marking point | Best file to open | Method |
|---|---|---|
| Packages | `README.md` structure, then `gui/MealsPanel.java` | imports at the top, `addMealItem()` |
| Interfaces (DAO) | `dao/MealDAO.java` and `dao/impl/MealDAOImpl.java` | whole interface |
| Interfaces (Refreshable) | `gui/DashboardFrame.java` | `showScreen()` |
| Interfaces (ReportExporter) | `gui/DashboardHomePanel.java` | `exportReport()` |
| Collections | `service/NutritionService.java` | `getLastSevenDays()`, `suggestFoods()` |
| java.time | `service/NutritionService.java` | `getLoggingStreak()` |
| java.io | `service/TextReportExporter.java` | `export()` |
| MessageDigest | `util/PasswordUtil.java` | `hash()` |
| Custom exceptions | `exception/` folder, `util/Validator.java` | `parseQuantity()` |
| try/catch/finally | `dao/impl/MealDAOImpl.java` | `addItem()` |
| Wrapping with cause | `dao/impl/UserDAOImpl.java` | `insert()` |
| Multi-catch | `gui/DashboardHomePanel.java` | `exportReport()` |
