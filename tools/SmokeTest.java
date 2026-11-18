import dao.FoodDAO;
import dao.GoalDAO;
import dao.MealDAO;
import dao.UserDAO;
import dao.WaterDAO;
import dao.impl.FoodDAOImpl;
import dao.impl.GoalDAOImpl;
import dao.impl.MealDAOImpl;
import dao.impl.UserDAOImpl;
import dao.impl.WaterDAOImpl;
import database.DBConnection;
import exception.AuthenticationException;
import exception.DuplicateEntryException;
import exception.InvalidInputException;
import model.DailySummary;
import model.Food;
import model.FoodSuggestion;
import model.Meal;
import model.NutritionGoal;
import model.User;
import model.WaterIntake;
import service.AuthService;
import service.CsvReportExporter;
import service.GoalCalculator;
import service.NutritionService;
import service.ReportExporter;
import service.TextReportExporter;
import util.PasswordUtil;
import util.Session;
import util.Validator;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Console test for NutriTrack. It calls every DAO and service method against
 * the real database and prints PASS or FAIL for each check.
 *
 * It works on its OWN temporary user and food, and deletes them at the end,
 * so the demo account's data is not changed.
 *
 * Compile and run from the project folder (after compiling the app into bin):
 *   macOS:   javac -d bin -cp "bin:lib/mysql-connector-j.jar" tools/SmokeTest.java
 *            java -cp "bin:lib/mysql-connector-j.jar" SmokeTest
 *   Windows: javac -d bin -cp "bin;lib\mysql-connector-j.jar" tools\SmokeTest.java
 *            java -cp "bin;lib\mysql-connector-j.jar" SmokeTest
 */
public class SmokeTest {

    private static final String DEMO_EMAIL = "student@nutritrack.com";
    private static final String DEMO_PASSWORD = "password123";

    private static final String TEST_EMAIL = "smoketest@nutritrack.test";
    private static final String TEST_PASSWORD = "secret42";
    private static final String TEST_FOOD_NAME = "SmokeTest Food";

    private static int passed = 0;
    private static int failed = 0;

    private static final UserDAO userDao = new UserDAOImpl();
    private static final FoodDAO foodDao = new FoodDAOImpl();
    private static final MealDAO mealDao = new MealDAOImpl();
    private static final GoalDAO goalDao = new GoalDAOImpl();
    private static final WaterDAO waterDao = new WaterDAOImpl();
    private static final AuthService authService = new AuthService();
    private static final NutritionService nutritionService = new NutritionService();

    // Created by the tests and shared between sections
    private static User testUser;
    private static Food testFood;

    /** A piece of test code that is allowed to throw. */
    private interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        System.out.println("NutriTrack smoke test");

        runSection("1. Utilities (no database)", new Action() {
            public void run() throws Exception {
                testUtilities();
            }
        });
        runSection("2. Goal calculator (no database)", new Action() {
            public void run() throws Exception {
                testGoalCalculator();
            }
        });

        if (!canConnect()) {
            System.out.println();
            System.out.println("Cannot reach the database, so the remaining checks were skipped.");
            System.out.println("Check that MySQL is running and that config/db.properties has your password.");
            printResult();
            return;
        }

        runSection("4. AuthService and UserDAO", new Action() {
            public void run() throws Exception {
                removeLeftovers();
                testAuthAndUsers();
            }
        });
        runSection("5. FoodDAO", new Action() {
            public void run() throws Exception {
                testFoods();
            }
        });
        runSection("6. MealDAO", new Action() {
            public void run() throws Exception {
                testMeals();
            }
        });
        runSection("7. WaterDAO", new Action() {
            public void run() throws Exception {
                testWater();
            }
        });
        runSection("8. GoalDAO", new Action() {
            public void run() throws Exception {
                testGoals();
            }
        });
        runSection("9. NutritionService", new Action() {
            public void run() throws Exception {
                testNutritionService();
            }
        });
        runSection("10. Report exporters", new Action() {
            public void run() throws Exception {
                testExporters();
            }
        });
        runSection("11. Deleting meal items", new Action() {
            public void run() throws Exception {
                testMealDeletes();
            }
        });
        runSection("12. Clean up", new Action() {
            public void run() throws Exception {
                testCleanUp();
            }
        });

        printResult();
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    private static void testUtilities() throws Exception {
        String hash = PasswordUtil.hash("password123");
        check("PasswordUtil.hash gives the known SHA-256 value",
                "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f".equals(hash));
        check("PasswordUtil.matches accepts the right password", PasswordUtil.matches("password123", hash));
        check("PasswordUtil.matches rejects a wrong password", !PasswordUtil.matches("password124", hash));

        check("Validator.parseQuantity accepts 150", Validator.parseQuantity("150") == 150);
        check("Validator.parseWaterAmount accepts 250", Validator.parseWaterAmount(" 250 ") == 250);
        check("Validator.requireEmail trims and lowercases",
                "a.b@example.com".equals(Validator.requireEmail("  A.B@Example.com ")));

        expectInvalid("Validator rejects empty text", new Action() {
            public void run() throws Exception {
                Validator.requireText("   ", "Name");
            }
        });
        expectInvalid("Validator rejects a non-number quantity", new Action() {
            public void run() throws Exception {
                Validator.parseQuantity("abc");
            }
        });
        expectInvalid("Validator rejects quantity 0", new Action() {
            public void run() throws Exception {
                Validator.parseQuantity("0");
            }
        });
        expectInvalid("Validator rejects a negative quantity", new Action() {
            public void run() throws Exception {
                Validator.parseQuantity("-20");
            }
        });
        expectInvalid("Validator rejects quantity above 2000 g", new Action() {
            public void run() throws Exception {
                Validator.parseQuantity("2500");
            }
        });
        expectInvalid("Validator rejects water above 5000 ml", new Action() {
            public void run() throws Exception {
                Validator.parseWaterAmount("6000");
            }
        });
        expectInvalid("Validator rejects water 12.5 (not a whole number)", new Action() {
            public void run() throws Exception {
                Validator.parseWaterAmount("12.5");
            }
        });
        expectInvalid("Validator rejects a bad email", new Action() {
            public void run() throws Exception {
                Validator.requireEmail("not-an-email");
            }
        });
        expectInvalid("Validator rejects a short password", new Action() {
            public void run() throws Exception {
                Validator.requirePassword("abc");
            }
        });
        expectInvalid("Validator rejects age 150", new Action() {
            public void run() throws Exception {
                Validator.parseAge("150");
            }
        });
    }

    private static void testGoalCalculator() {
        // Same person as the demo user: male, 20 years, 175 cm, 68.5 kg, Moderate
        User person = new User(0, "Calc", "calc@test", "", 20, "Male", 175, 68.5, "Moderate");

        double bmi = GoalCalculator.calculateBmi(68.5, 175);
        check("BMI of 68.5 kg / 175 cm is about 22.4", close(bmi, 22.37, 0.01));
        check("BMI 22.4 is Normal", "Normal".equals(GoalCalculator.getBmiCategory(bmi)));
        check("BMI 17 is Underweight", "Underweight".equals(GoalCalculator.getBmiCategory(17)));
        check("BMI 27 is Overweight", "Overweight".equals(GoalCalculator.getBmiCategory(27)));
        check("BMI 32 is Obese", "Obese".equals(GoalCalculator.getBmiCategory(32)));

        // 10*68.5 + 6.25*175 - 5*20 + 5 = 1683.75
        check("BMR (Mifflin-St Jeor) is 1683.75", close(GoalCalculator.calculateBmr(person), 1683.75, 0.001));
        // 1683.75 * 1.55 = 2609.81
        check("Daily calories = BMR x 1.55", close(GoalCalculator.calculateDailyCalories(person), 2609.81, 0.01));

        NutritionGoal maintain = GoalCalculator.suggestGoals(person, GoalCalculator.GOAL_MAINTAIN);
        NutritionGoal lose = GoalCalculator.suggestGoals(person, GoalCalculator.GOAL_LOSE);
        NutritionGoal gain = GoalCalculator.suggestGoals(person, GoalCalculator.GOAL_GAIN);
        check("Maintain goal is 2610 kcal", maintain.getTargetCalories() == 2610);
        check("Lose goal is 500 kcal lower", lose.getTargetCalories() == 2110);
        check("Gain goal is 500 kcal higher", gain.getTargetCalories() == 3110);
        check("Protein goal is 25% of calories / 4", close(maintain.getTargetProtein(), 163, 0.5));
        check("Carbs goal is 50% of calories / 4", close(maintain.getTargetCarbs(), 326, 0.5));
        check("Fat goal is 25% of calories / 9", close(maintain.getTargetFat(), 72, 0.5));
        check("Water goal is 35 ml per kg (2400 ml)", maintain.getTargetWater() == 2400);

        User small = new User(0, "Small", "small@test", "", 60, "Female", 150, 40, "Sedentary");
        check("Calorie goal never goes below 1200",
                GoalCalculator.suggestGoals(small, GoalCalculator.GOAL_LOSE).getTargetCalories() == 1200);
    }

    private static boolean canConnect() {
        System.out.println();
        System.out.println("3. Database connection");
        try (Connection conn = DBConnection.getConnection()) {
            check("DBConnection.getConnection opens a connection", conn != null && !conn.isClosed());
            return true;
        } catch (Exception e) {
            check("DBConnection.getConnection opens a connection (" + e.getMessage() + ")", false);
            return false;
        }
    }

    /** If an earlier run crashed, its test user and food may still be there. */
    private static void removeLeftovers() throws Exception {
        User oldUser = userDao.findByEmail(TEST_EMAIL);
        if (oldUser != null) {
            userDao.delete(oldUser.getUserId());
        }
        Food oldFood = foodDao.findByName(TEST_FOOD_NAME);
        if (oldFood != null) {
            foodDao.delete(oldFood.getFoodId());
        }
    }

    private static void testAuthAndUsers() throws Exception {
        User demo = authService.login(DEMO_EMAIL, DEMO_PASSWORD);
        check("Demo user can log in", demo != null && DEMO_EMAIL.equals(demo.getEmail()));
        check("Login stores the user in Session", Session.isLoggedIn() && Session.getUserId() == demo.getUserId());
        check("Demo user has gender and activity level", demo.getGender() != null && demo.getActivityLevel() != null);

        authService.logout();
        check("Logout clears the Session", !Session.isLoggedIn());

        expectThrows("Wrong password is rejected", AuthenticationException.class, new Action() {
            public void run() throws Exception {
                authService.login(DEMO_EMAIL, "wrong-password");
            }
        });
        expectThrows("Unknown email is rejected", AuthenticationException.class, new Action() {
            public void run() throws Exception {
                authService.login("nobody@nutritrack.test", "whatever1");
            }
        });
        expectInvalid("Empty password is rejected before reaching the database", new Action() {
            public void run() throws Exception {
                authService.login(DEMO_EMAIL, "");
            }
        });
        check("A failed login leaves nobody logged in", !Session.isLoggedIn());

        testUser = authService.register(newTestUser(), TEST_PASSWORD);
        check("Sign-up returns a user with a new id", testUser.getUserId() > 0);

        User stored = userDao.findByEmail(TEST_EMAIL);
        check("UserDAO.findByEmail finds the new user", stored != null && stored.getUserId() == testUser.getUserId());
        check("Password is stored as a 64-character hash, not as plain text",
                stored != null && stored.getPassword().length() == 64 && !TEST_PASSWORD.equals(stored.getPassword()));
        check("UserDAO.findByEmail returns null for an unknown email",
                userDao.findByEmail("nobody@nutritrack.test") == null);
        check("Sign-up creates starting goals", goalDao.findByUser(testUser.getUserId()) != null);

        expectThrows("Signing up twice with the same email is rejected", DuplicateEntryException.class, new Action() {
            public void run() throws Exception {
                authService.register(newTestUser(), TEST_PASSWORD);
            }
        });
        expectInvalid("Sign-up with a short password is rejected", new Action() {
            public void run() throws Exception {
                User other = newTestUser();
                other.setEmail("other@nutritrack.test");
                authService.register(other, "123");
            }
        });

        User loggedIn = authService.login(TEST_EMAIL, TEST_PASSWORD);
        check("The new user can log in", loggedIn.getUserId() == testUser.getUserId());

        testUser.setName("Smoke Tester");
        testUser.setWeight(72.5);
        testUser.setActivityLevel("Active");
        userDao.updateProfile(testUser);
        User updated = userDao.findByEmail(TEST_EMAIL);
        check("UserDAO.updateProfile saves the changes",
                "Smoke Tester".equals(updated.getName()) && close(updated.getWeight(), 72.5, 0.001)
                        && "Active".equals(updated.getActivityLevel()));
    }

    private static void testFoods() throws Exception {
        List<Food> before = foodDao.findAll();
        check("FoodDAO.findAll returns the food library (" + before.size() + " foods)", before.size() >= 10);

        boolean sorted = true;
        for (int i = 1; i < before.size(); i++) {
            if (before.get(i - 1).getFoodName().compareToIgnoreCase(before.get(i).getFoodName()) > 0) {
                sorted = false;
            }
        }
        check("FoodDAO.findAll is sorted by name", sorted);

        // 200 kcal, 20 g protein, 10 g carbs, 5 g fat per 100 g
        Food food = new Food(0, TEST_FOOD_NAME, "Test", 200, 20, 10, 5);
        int newId = foodDao.insert(food);
        check("FoodDAO.insert returns a new id", newId > 0);

        testFood = foodDao.findByName(TEST_FOOD_NAME);
        check("FoodDAO.findByName finds the new food", testFood != null && testFood.getFoodId() == newId);
        check("FoodDAO.findByName returns null for an unknown food", foodDao.findByName("No Such Food 123") == null);
        check("FoodDAO.findAll now has one more food", foodDao.findAll().size() == before.size() + 1);

        expectThrows("Adding the same food name twice is rejected", DuplicateEntryException.class, new Action() {
            public void run() throws Exception {
                foodDao.insert(new Food(0, TEST_FOOD_NAME, "Test", 1, 1, 1, 1));
            }
        });

        testFood.setCategory("Test Updated");
        foodDao.update(testFood);
        check("FoodDAO.update saves the changes",
                "Test Updated".equals(foodDao.findByName(TEST_FOOD_NAME).getCategory()));

        final Food firstFood = before.get(0);
        expectThrows("Renaming a food to an existing name is rejected", DuplicateEntryException.class, new Action() {
            public void run() throws Exception {
                Food clash = new Food(testFood.getFoodId(), firstFood.getFoodName(), "Test", 1, 1, 1, 1);
                foodDao.update(clash);
            }
        });

        check("FoodDAO.countUsages is 0 for an unused food", foodDao.countUsages(testFood.getFoodId()) == 0);
    }

    private static void testMeals() throws Exception {
        int userId = testUser.getUserId();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        check("A new user has no meals today", mealDao.findByDate(userId, today).isEmpty());

        // Two Breakfast items on the same day must share ONE meal
        mealDao.addItem(userId, "Breakfast", today, testFood.getFoodId(), 150);
        mealDao.addItem(userId, "Breakfast", today, testFood.getFoodId(), 50);
        List<Meal> meals = mealDao.findByDate(userId, today);
        check("Two Breakfast items reuse one meal", meals.size() == 1 && meals.get(0).getItems().size() == 2);
        check("Meal items come back with their Food filled in",
                meals.get(0).getItems().get(0).getFood() != null
                        && TEST_FOOD_NAME.equals(meals.get(0).getItems().get(0).getFood().getFoodName()));
        // 150 g of a 200 kcal food = 300 kcal
        check("MealItem.getCalories = calories x quantity / 100",
                close(meals.get(0).getItems().get(0).getCalories(), 300, 0.001));

        mealDao.addItem(userId, "Dinner", today, testFood.getFoodId(), 100);
        meals = mealDao.findByDate(userId, today);
        check("A different meal type creates a second meal", meals.size() == 2);
        check("Meals are ordered Breakfast before Dinner",
                "Breakfast".equals(meals.get(0).getMealType()) && "Dinner".equals(meals.get(1).getMealType()));

        mealDao.addItem(userId, "Lunch", yesterday, testFood.getFoodId(), 250);
        check("Yesterday's meal does not appear in today's list", mealDao.findByDate(userId, today).size() == 2);
        check("Yesterday's meal is found by its own date", mealDao.findByDate(userId, yesterday).size() == 1);

        // today 150 + 50 + 100 = 300 g -> 600 kcal; yesterday 250 g -> 500 kcal
        List<DailySummary> totals = mealDao.findDailyTotals(userId, yesterday, today);
        check("MealDAO.findDailyTotals returns one row per logged day", totals.size() == 2);
        check("Daily totals are correct (500 kcal yesterday, 600 kcal today)",
                totals.size() == 2 && close(totals.get(0).getCalories(), 500, 0.01)
                        && close(totals.get(1).getCalories(), 600, 0.01));

        List<LocalDate> dates = mealDao.findLoggedDates(userId);
        check("MealDAO.findLoggedDates returns both days, newest first",
                dates.size() == 2 && dates.get(0).equals(today) && dates.get(1).equals(yesterday));

        check("FoodDAO.countUsages counts the 4 items", foodDao.countUsages(testFood.getFoodId()) == 4);
    }

    private static void testWater() throws Exception {
        int userId = testUser.getUserId();
        LocalDate today = LocalDate.now();

        check("A new user has 0 ml today", waterDao.getTotalForDate(userId, today) == 0);

        int firstId = waterDao.insert(new WaterIntake(0, userId, 250, LocalDateTime.now()));
        waterDao.insert(new WaterIntake(0, userId, 500, LocalDateTime.now()));
        // Yesterday's water must not be counted in today's total
        waterDao.insert(new WaterIntake(0, userId, 1000, LocalDateTime.now().minusDays(1)));

        check("WaterDAO.insert returns a new id", firstId > 0);
        check("WaterDAO.findByDate returns today's 2 entries", waterDao.findByDate(userId, today).size() == 2);
        check("WaterDAO.getTotalForDate adds up today only (750 ml)", waterDao.getTotalForDate(userId, today) == 750);
        check("Yesterday's total is separate (1000 ml)",
                waterDao.getTotalForDate(userId, today.minusDays(1)) == 1000);

        waterDao.delete(firstId);
        check("WaterDAO.delete removes the entry (500 ml left)", waterDao.getTotalForDate(userId, today) == 500);
    }

    private static void testGoals() throws Exception {
        int userId = testUser.getUserId();

        NutritionGoal goal = new NutritionGoal(0, userId, 2000, 100, 250, 60, 3000);
        goalDao.save(goal);
        NutritionGoal saved = goalDao.findByUser(userId);
        check("GoalDAO.save updates the existing goals",
                saved.getTargetCalories() == 2000 && close(saved.getTargetProtein(), 100, 0.001)
                        && saved.getTargetWater() == 3000);

        goal.setTargetCalories(1800);
        goalDao.save(goal);
        check("Saving again changes the same row", goalDao.findByUser(userId).getTargetCalories() == 1800);
        check("GoalDAO.findByUser returns null for a user without goals", goalDao.findByUser(-1) == null);
        check("NutritionService.getGoals gives default goals instead of null",
                nutritionService.getGoals(-1).getTargetCalories() == 2000);

        // Back to 2000 kcal for the NutritionService checks below
        goal.setTargetCalories(2000);
        goalDao.save(goal);
    }

    private static void testNutritionService() throws Exception {
        int userId = testUser.getUserId();
        LocalDate today = LocalDate.now();

        // Today: 300 g of the test food (200 kcal, 20 P, 10 C, 5 F per 100 g) and 500 ml water
        DailySummary summary = nutritionService.getDailySummary(userId, today);
        check("getDailySummary: 600 kcal today", close(summary.getCalories(), 600, 0.01));
        check("getDailySummary: 60 g protein, 30 g carbs, 15 g fat",
                close(summary.getProtein(), 60, 0.01) && close(summary.getCarbs(), 30, 0.01)
                        && close(summary.getFat(), 15, 0.01));
        check("getDailySummary: 500 ml water", summary.getWaterMl() == 500);

        DailySummary emptyDay = nutritionService.getDailySummary(userId, today.minusDays(30));
        check("getDailySummary of an empty day is all zero", emptyDay.getCalories() == 0 && !emptyDay.hasMeals());

        Map<String, Double> byType = nutritionService.getCaloriesByMealType(mealDao.findByDate(userId, today));
        check("getCaloriesByMealType has all 4 meal types", byType.size() == 4);
        check("getCaloriesByMealType: Breakfast 400, Dinner 200, Lunch 0",
                close(byType.get("Breakfast"), 400, 0.01) && close(byType.get("Dinner"), 200, 0.01)
                        && byType.get("Lunch") == 0);

        Map<LocalDate, DailySummary> week = nutritionService.getLastSevenDays(userId);
        check("getLastSevenDays has exactly 7 days", week.size() == 7);
        LocalDate firstKey = week.keySet().iterator().next();
        check("getLastSevenDays starts 6 days ago and includes today",
                firstKey.equals(today.minusDays(6)) && week.containsKey(today));
        check("Days without meals are filled in as empty", !week.get(today.minusDays(3)).hasMeals());

        // Logged days: yesterday 500 kcal, today 600 kcal -> average 550
        check("getAverageCalories skips empty days (550 kcal)",
                close(nutritionService.getAverageCalories(week), 550, 0.01));
        // Goal 2000: today (600) is closer than yesterday (500)
        DailySummary best = nutritionService.getBestDay(week, 2000);
        check("getBestDay picks the day closest to the goal", best != null && best.getDate().equals(today));
        check("getLoggingStreak counts today and yesterday (2)", nutritionService.getLoggingStreak(userId) == 2);

        // Goals 2000 kcal / 100 P / 250 C / 60 F; eaten 600 / 60 / 30 / 15
        // Missing: calories 70%, protein 40%, carbs 88%, fat 75% -> carbs
        NutritionGoal goal = nutritionService.getGoals(userId);
        String gap = nutritionService.findBiggestGap(summary, goal);
        check("findBiggestGap picks Carbs (88% missing)", NutritionService.CARBS.equals(gap));

        List<FoodSuggestion> suggestions = nutritionService.suggestFoods(gap, summary, goal);
        check("suggestFoods returns 3 foods", suggestions.size() == 3);
        boolean ranked = true;
        boolean sensible = true;
        for (int i = 0; i < suggestions.size(); i++) {
            FoodSuggestion s = suggestions.get(i);
            if (i > 0 && suggestions.get(i - 1).getFood().getCarbs() < s.getFood().getCarbs()) {
                ranked = false;
            }
            if (s.getGrams() < 10 || s.getGrams() > 150 || s.getNutrientAmount() <= 0) {
                sensible = false;
            }
        }
        check("Suggestions are ranked richest first", ranked);
        check("Suggested amounts are between 10 g and 150 g", sensible);

        DailySummary full = new DailySummary(today, 5000, 500, 500, 500);
        check("findBiggestGap returns null when every goal is met", nutritionService.findBiggestGap(full, goal) == null);
        check("suggestFoods returns nothing when the goal is met",
                nutritionService.suggestFoods(NutritionService.PROTEIN, full, goal).isEmpty());
    }

    private static void testExporters() throws Exception {
        int userId = testUser.getUserId();
        LocalDate today = LocalDate.now();
        DailySummary summary = nutritionService.getDailySummary(userId, today);
        NutritionGoal goal = nutritionService.getGoals(userId);
        List<Meal> meals = mealDao.findByDate(userId, today);

        File folder = new File(System.getProperty("java.io.tmpdir"), "nutritrack_smoketest");

        // The same loop works for every exporter because they share one interface
        ReportExporter[] exporters = {new TextReportExporter(), new CsvReportExporter()};
        for (ReportExporter exporter : exporters) {
            File file = exporter.export(testUser, summary, goal, meals, folder);
            check(exporter.getFormatName() + " export creates a non-empty file (" + file.getName() + ")",
                    file.exists() && file.length() > 100);
            file.delete();
        }
        folder.delete();
    }

    private static void testMealDeletes() throws Exception {
        int userId = testUser.getUserId();
        LocalDate today = LocalDate.now();

        // Breakfast has 2 items, Dinner has 1
        List<Meal> meals = mealDao.findByDate(userId, today);
        Meal breakfast = meals.get(0);
        Meal dinner = meals.get(1);

        mealDao.deleteItem(breakfast.getItems().get(0).getMealItemId());
        meals = mealDao.findByDate(userId, today);
        check("Deleting one of two items keeps the meal",
                meals.size() == 2 && meals.get(0).getItems().size() == 1);

        mealDao.deleteItem(dinner.getItems().get(0).getMealItemId());
        meals = mealDao.findByDate(userId, today);
        check("Deleting the last item removes the meal too",
                meals.size() == 1 && "Breakfast".equals(meals.get(0).getMealType()));

        mealDao.deleteItem(-999);
        check("Deleting an item that does not exist is harmless", mealDao.findByDate(userId, today).size() == 1);
    }

    private static void testCleanUp() throws Exception {
        int userId = testUser.getUserId();

        foodDao.delete(testFood.getFoodId());
        check("FoodDAO.delete removes the test food", foodDao.findByName(TEST_FOOD_NAME) == null);

        userDao.delete(userId);
        check("UserDAO.delete removes the test user", userDao.findByEmail(TEST_EMAIL) == null);
        check("The user's water and goals were removed with them",
                waterDao.getTotalForDate(userId, LocalDate.now()) == 0 && goalDao.findByUser(userId) == null);

        authService.logout();
        User demo = userDao.findByEmail(DEMO_EMAIL);
        check("The demo user is still there", demo != null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static User newTestUser() {
        return new User(0, "Smoke Test", TEST_EMAIL, null, 21, "Female", 165, 60, "Light");
    }

    private static void runSection(String title, Action section) {
        System.out.println();
        System.out.println(title);
        try {
            section.run();
        } catch (Exception e) {
            // An unexpected exception fails the section but the other sections still run
            check("Section stopped by " + e.getClass().getSimpleName() + ": " + e.getMessage(), false);
            if (e.getCause() != null) {
                System.out.println("         cause: " + e.getCause());
            }
        }
    }

    private static void check(String description, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description);
        }
    }

    private static void expectInvalid(String description, Action action) {
        expectThrows(description, InvalidInputException.class, action);
    }

    /** Passes only if the action throws an exception of the expected type. */
    private static void expectThrows(String description, Class<? extends Exception> expected, Action action) {
        try {
            action.run();
            check(description + " (no exception was thrown)", false);
        } catch (Exception e) {
            if (expected.isInstance(e)) {
                check(description, true);
            } else {
                check(description + " (got " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", false);
            }
        }
    }

    private static boolean close(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    private static void printResult() {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.out.println(failed == 0 ? "ALL CHECKS PASSED" : "SOME CHECKS FAILED");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
