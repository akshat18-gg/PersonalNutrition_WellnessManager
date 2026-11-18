# Review 3 – Viva Notes

Topic: **Software application development** (the complete app and its special features) and the **demo**.

Contents:
1. What changed since Review 1
2. Each feature: what it does, how it works, which files
3. The formulas (with the demo user's real numbers)
4. A 5-minute demo script, click by click
5. Before the demo: checklist
6. Likely questions

---

## 1. What changed since Review 1

| Problem in Review 1 | How it is fixed | Where |
|---|---|---|
| Login opened the dashboard without checking anything; no sign-up | Real email + password check, and a sign-up screen | `service/AuthService.java`, `gui/LoginFrame.java`, `gui/SignUpFrame.java` |
| `user_id = 1` hardcoded in every query | Every query takes the id of the logged-in user | `util/Session.java` (`Session.getUserId()`), all DAO methods have a `userId` parameter |
| Dashboard added up all days | Totals are for today only | `NutritionService.getDailySummary(userId, LocalDate.now())` |
| Model classes never used, SQL inside the screens | DAOs return model objects; screens have no SQL | `dao/`, `dao/impl/`, `model/` |
| Database password hardcoded | Read from a properties file | `database/DBConnection.java`, `config/db.properties` |
| Demo text in popups ("Verify in MySQL CMD...") | Normal user messages | all `gui` files, `UIStyles.showInfo / showWarning / showError` |
| Every "Add Meal" made a new meal with one item | The day's meal of that type is reused; deleting removes one item, and the meal too if it becomes empty | `MealDAOImpl.addItem()`, `MealDAOImpl.deleteItem()` |

---

## 2. Features

### Feature 1 – Real login and sign-up

**What it does:** Users create an account and log in. Each user sees only their own meals, water and goals. The dashboard greets them by name.

**How it works:** At sign-up, `AuthService.register()` validates the input, hashes the password with SHA-256 and saves the user through `UserDAO`. At login, `AuthService.login()` finds the user by email, hashes the typed password and compares it with the stored hash. On success the user object is kept in `Session`, and every screen asks `Session.getUserId()` when it loads data.

**Files:** `service/AuthService.java`, `util/PasswordUtil.java`, `util/Session.java`, `util/Validator.java`, `dao/impl/UserDAOImpl.java`, `gui/LoginFrame.java`, `gui/SignUpFrame.java`

**Nice details to mention:**
- The error is the same for "unknown email" and "wrong password", so nobody can find out which emails are registered.
- A second sign-up with the same email is stopped by the `UNIQUE` column in MySQL and becomes a `DuplicateEntryException`.
- A new user automatically gets starting goals from the goal calculator.

---

### Feature 2 – Dashboard with progress bars

**What it does:** Five cards (calories, protein, carbs, fat, water) show today's amount against the goal with a coloured bar.

**How it works:** `DashboardHomePanel.refreshData()` asks `NutritionService` for today's `DailySummary` and the user's `NutritionGoal`. Each `MetricCard` works out the percentage and picks the colour with `UIStyles.getProgressColor()`: **green below 80%, orange from 80% to 100%, red above 100%**.

**Files:** `gui/DashboardHomePanel.java`, `gui/MetricCard.java`, `gui/UIStyles.java`, `service/NutritionService.java`, `dao/impl/MealDAOImpl.java` (`findDailyTotals`)

**Nice detail:** Water is a target to reach, not a limit, so its bar stays green and says "Goal reached" instead of turning red.

---

### Feature 3 – Smart Goal Calculator (Profile screen)

**What it does:** Shows the user's BMI and its category, their BMR and daily calorie need. "Suggest my goals" calculates calories, protein, carbs, fat and water for losing, maintaining or gaining weight, and fills the Goals form in one click.

**How it works:** `GoalCalculator` has only static maths methods. It calculates BMR with the Mifflin-St Jeor equation, multiplies by an activity factor, adds or subtracts 500 kcal for the chosen goal, and splits the calories into macros. `ProfilePanel` passes the result to `DashboardFrame.showSuggestedGoals()`, which opens the Goals screen with the numbers filled in. Nothing is saved until the user presses "Save Goals".

**Files:** `service/GoalCalculator.java`, `gui/ProfilePanel.java`, `gui/GoalsPanel.java` (`showSuggestedGoals`), `gui/DashboardFrame.java`

The formulas are in section 3 below.

---

### Feature 4 – "What should I eat next?"

**What it does:** A card on the dashboard names the nutrient you are furthest behind on today and lists three foods rich in it, with how many grams to eat.

**How it works:** `NutritionService.findBiggestGap()` calculates the *percentage still missing* for calories, protein, carbs and fat, and picks the biggest. `suggestFoods()` then sorts the whole food library by that nutrient using a `Comparator` (highest first), takes the top 3, and calculates the grams needed to fill the gap: `grams = remaining / (value per 100 g) x 100`, rounded to 10 g and capped at 150 g.

**Files:** `service/NutritionService.java` (`findBiggestGap`, `suggestFoods`), `model/FoodSuggestion.java`, `gui/DashboardHomePanel.java` (`showSuggestions`)

**Why percentage?** Calories are in kcal and protein in grams, so the raw numbers cannot be compared. "61% of protein still missing" and "47% of calories still missing" can.

---

### Feature 5 – Weekly report with a hand-drawn chart

**What it does:** A bar chart of calories for the last 7 days with a dashed line for the goal. Below it: the weekly average, the best day, and the logging streak.

**How it works:** `NutritionService.getLastSevenDays()` builds a `LinkedHashMap` with one entry per day (empty days are filled with zero). `CalorieChartPanel.paintComponent()` draws everything with `Graphics2D`: grid lines, one `fillRect` per bar, and a dashed `BasicStroke` line for the goal. No chart library is used.

- **Weekly average** = total calories / number of days that have meals.
- **Best day** = the day whose calories are closest to the goal.
- **Streak** = days in a row, counting back from today, with at least one meal. If today has nothing yet, counting starts from yesterday.

**Files:** `gui/WeeklyReportPanel.java`, `gui/CalorieChartPanel.java`, `service/NutritionService.java` (`getLastSevenDays`, `getAverageCalories`, `getBestDay`, `getLoggingStreak`), `dao/impl/MealDAOImpl.java` (`findDailyTotals`, `findLoggedDates`)

**How a bar's height is calculated (good viva point):**
```text
y = baseY - (calories / maxValue) x chartHeight
```
Pixels count from the top of the panel, so a bigger value means a *smaller* y. See `CalorieChartPanel.toY()`.

---

### Feature 6 – Export report

**What it does:** "Export Today's Report" saves today's summary and meals as a `.txt` or `.csv` file in the `reports/` folder and shows the full path.

**How it works:** `ReportExporter` is an interface with two implementations. `DashboardHomePanel.exportReport()` puts both in an array, lets the user pick one, and calls `exporter.export(...)`. Both write the file with `BufferedWriter` and `FileWriter` inside try-with-resources.

**Files:** `service/ReportExporter.java`, `service/TextReportExporter.java`, `service/CsvReportExporter.java`, `gui/DashboardHomePanel.java`

---

### Feature 7 – Small polish

| Polish | How it works | Where |
|---|---|---|
| Search box on Food Library | A `DocumentListener` runs on every keystroke and rebuilds the table with only the foods whose name or category contains the text | `FoodLibraryPanel.createSearchBar()`, `showFilteredFoods()` |
| Quick +250 / +500 / +750 ml | A loop creates three buttons from an array; each calls `saveWater(amount)` | `WaterPanel.createFormPanel()` |
| Date picker on Meals | A `JSpinner` with a date model, plus Previous Day / Next Day / Today buttons. You can also log a meal for a past day. | `MealsPanel.createDateBar()` |
| Sidebar highlight | The open screen's button turns light green | `DashboardFrame.showScreen()`, `UIStyles.styleNavButton()` |
| Warning before deleting a food | Tells you how many logged meal items use it | `FoodLibraryPanel.deleteSelectedFood()`, `FoodDAO.countUsages()` |
| Calories per meal type | A line under the Meals table: Breakfast / Lunch / Snack / Dinner / Total | `MealsPanel.showTotals()`, `NutritionService.getCaloriesByMealType()` |

---

## 3. The formulas

All in `service/GoalCalculator.java`. The numbers on the right are for the demo user (male, 20 years, 175 cm, 68.5 kg, Moderate activity), so you can check them on the Profile screen.

| What | Formula | Demo user |
|---|---|---|
| BMI | weight (kg) / height (m)² | 68.5 / 1.75² = **22.4** |
| BMI category | below 18.5 Underweight, 18.5–24.9 Normal, 25–29.9 Overweight, 30+ Obese | **Normal** |
| BMR (Mifflin-St Jeor) | 10 × weight + 6.25 × height − 5 × age + s, where s = +5 for men, −161 for women | 685 + 1093.75 − 100 + 5 = **1684 kcal** |
| Activity factor | Sedentary 1.2, Light 1.375, Moderate 1.55, Active 1.725, Very Active 1.9 | 1.55 |
| Daily calories | BMR × activity factor | 1683.75 × 1.55 = **2610 kcal** |
| Goal adjustment | Lose: −500 kcal, Maintain: 0, Gain: +500 kcal (never below 1200) | Lose 2110, Maintain 2610, Gain 3110 |
| Protein | 25% of calories ÷ 4 (1 g protein = 4 kcal) | 2610 × 0.25 / 4 = **163 g** |
| Carbs | 50% of calories ÷ 4 (1 g carbs = 4 kcal) | 2610 × 0.50 / 4 = **326 g** |
| Fat | 25% of calories ÷ 9 (1 g fat = 9 kcal) | 2610 × 0.25 / 9 = **72 g** |
| Water | 35 ml × weight (kg), rounded to 50 ml | 68.5 × 35 = **2400 ml** |

Other formulas in the app:

| What | Formula | Where |
|---|---|---|
| Nutrition of a meal item | value per 100 g × quantity ÷ 100 | `MealItem.getCalories()` and the SQL in `MealDAOImpl.findDailyTotals()` |
| Progress percent | consumed ÷ goal × 100 | `MetricCard.update()` |
| Percent missing | (goal − consumed) ÷ goal × 100 | `NutritionService.percentMissing()` |
| Suggested grams | remaining ÷ value per 100 g × 100, rounded to 10 g, max 150 g | `NutritionService.suggestFoods()` |

---

## 4. Five-minute demo script

**Before you start:** run `sql/nutritrack_schema.sql` that morning (see section 5), then start the app with `run.bat` or `run.sh`.

With fresh sample data the dashboard starts at about: Calories 1170 / 2200 (53%), Protein 46 / 120 (39%), Carbs 168 / 250 (67%), Fat 35 / 70 (50%), Water 1250 / 2500 (50%). All bars green.

| Time | Click | Say |
|---|---|---|
| **0:00** | On the login screen type `student@nutritrack.com` and a **wrong** password. Click **Login**. | "Login is real now. The password is hashed with SHA-256 and compared with the hash in the database." (Warning: *Incorrect email or password.*) |
| 0:20 | Type `password123`. Press **Enter**. | "The logged-in user is stored in our `Session` class, and every query uses that user's id." |
| **0:35** | Dashboard is showing. Point at the greeting and the five cards. | "The dashboard shows today only. Each card is a progress bar: green when on track, orange near the goal, red when over." |
| 0:55 | Point at the **What should I eat next?** card. | "The app found that protein is my biggest gap today. It sorted the food library with a Comparator and suggests the three foods richest in protein, with grams." |
| **1:15** | Click **Meals**. Point at the table and the totals line at the bottom. | "These are today's meals. Lunch is one meal with three items." |
| 1:30 | Meal Type **Snack**, Select Food **Soya Chunks**, Quantity `100`. Click **Add to Meal**. | "I follow the suggestion. If I add another snack item, it goes into the same Snack meal instead of creating a new one." |
| 1:45 | Quantity: type `abc`, click **Add to Meal**. Then type `-5`, click again. | "All input goes through our `Validator` class, which throws an `InvalidInputException`." (Two warnings.) |
| 2:00 | Select Food **Almonds**, Quantity `80`, **Add to Meal**. Then click **< Previous Day** once, then **Today**. | "I can look back at any day with the date picker." |
| **2:20** | Click **Dashboard**. | "Protein, carbs and calories are orange now because they are above 80%. Fat is red because the almonds took me over the limit. And the suggestion card has changed." |
| **2:40** | Click **Water**. Click **+250 ml**. Then type `6000` and click **Add Water**. | "Quick buttons for common sizes. More than 5000 ml in one entry is rejected as unrealistic." |
| **3:00** | Click **Food Library**. Type `dairy` in the search box. Clear it. | "The table filters while I type, using a DocumentListener." |
| 3:15 | In the add form type Food Name `Rice`, Category `Grains`, then `130`, `2.7`, `28`, `0.3`. Click **Add Food**. | "Rice already exists, so we get a `DuplicateEntryException` shown as a friendly message." |
| **3:30** | Click **Profile**. Point at Body Stats. | "BMI 22.4, Normal. BMR uses the Mifflin-St Jeor equation." |
| 3:45 | Choose **Lose weight**. Click **Suggest my goals**. | "It calculates calories, macros and water and fills the Goals form in one click. Nothing is saved until I press Save Goals." (Then click **Undo Changes** to keep the demo goals.) |
| **4:05** | Click **Weekly Report**. | "This chart is drawn by hand with Graphics2D in `paintComponent`, no library. The dashed line is my goal. Below: weekly average, best day, and a 7-day logging streak." |
| **4:30** | Click **Dashboard**, then **Export Today's Report**, choose **Text file (.txt)**. | "Export uses our `ReportExporter` interface. Text and CSV are two implementations and the calling code is the same." (Dialog shows where the file was saved.) |
| 4:45 | Click **Logout**, then **Create a new account** (just show the form). | "Sign-up collects the details the goal calculator needs. A new user starts with an empty dashboard and their own suggested goals." |
| **5:00** | Done. | |

**If you have 30 seconds extra:** open the exported file from the `reports` folder, or actually create an account and show that the new user's dashboard is empty.

**After practising:** the meals and water you added stay in the database. Run the SQL script again to reset the demo data.

---

## 5. Before the demo: checklist

1. MySQL is running.
2. `config/db.properties` has the password of **this** computer's MySQL.
3. Run `sql/nutritrack_schema.sql` **on the day of the demo**. The sample data is created relative to the current date, so old data makes the chart look empty and the streak 0.
4. Run the smoke test (see README section 5). The last line must be `ALL CHECKS PASSED`.
5. Start the app from the project folder with `run.bat` (Windows) or `bash run.sh` (macOS).
6. Log in once with the demo account to make sure everything is fine.
7. Keep these files open in the editor for code questions: `MealDAOImpl.java`, `NutritionService.java`, `GoalCalculator.java`, `Validator.java`, `DashboardFrame.java`, `CalorieChartPanel.java`.

---

## 6. Likely questions

**Q1. How does the app know which user's data to show?**
After login, `AuthService` stores the `User` object in `Session`. Every screen calls `Session.getUserId()` and passes it to the DAO, and every SQL query has `WHERE user_id = ?`.

**Q2. How do you make sure "Add to Meal" does not create a new meal every time?**
`MealDAOImpl.addItem()` first looks for a meal with the same user, type and date (`findMealId`). Only if there is none does it insert one. Then it inserts the item. Both steps are inside one transaction.

**Q3. How is the bar chart drawn?**
In `CalorieChartPanel.paintComponent()`. We work out the chart area from the panel's width and height, then for each day draw a rectangle with `fillRect`. The height is `calories / maxValue x chartHeight`. The goal line is `drawLine` with a dashed `BasicStroke`. Because sizes come from `getWidth()` and `getHeight()`, the chart resizes with the window.

**Q4. Why is the suggestion capped at 150 g?**
Without a cap, a big gap could produce "eat 400 g of peanuts", which is not sensible. 150 g is a realistic single serving. The suggestion is a simple rule, not medical advice.

**Q5. Where do the BMR numbers come from? Are they accurate?**
The Mifflin-St Jeor equation is a standard published formula for estimating resting calories, and the activity factors are the standard multipliers used with it. They are estimates for an average person, which is fine for setting a starting goal.

**Q6. What happens if the database is down?**
`Main.checkDatabase()` shows a clear message at startup with three things to check. Later, any DAO call throws `DatabaseException` and the screen shows a friendly dialog. The app does not crash.

**Q7. Why no frameworks or libraries?**
The only external jar is the MySQL JDBC driver. Everything else (hashing, dates, file writing, the chart) uses the JDK, so we can explain every line.

**Q8. What would you add next?**
Salted password hashing (bcrypt), a monthly report, editing the quantity of a logged item, and charts for protein and water as well as calories.
