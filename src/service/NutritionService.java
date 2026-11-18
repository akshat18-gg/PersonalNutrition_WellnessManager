package service;

import dao.FoodDAO;
import dao.GoalDAO;
import dao.MealDAO;
import dao.WaterDAO;
import dao.impl.FoodDAOImpl;
import dao.impl.GoalDAOImpl;
import dao.impl.MealDAOImpl;
import dao.impl.WaterDAOImpl;
import exception.DatabaseException;
import model.DailySummary;
import model.Food;
import model.FoodSuggestion;
import model.Meal;
import model.NutritionGoal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "thinking" part of the app: daily totals, the weekly report, the
 * logging streak and the food suggestions. It gets raw data from the DAOs
 * and turns it into the numbers that the screens show.
 */
public class NutritionService {

    public static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Snack", "Dinner"};

    public static final String CALORIES = "Calories";
    public static final String PROTEIN = "Protein";
    public static final String CARBS = "Carbs";
    public static final String FAT = "Fat";

    private static final int SUGGESTION_COUNT = 3;
    // A suggestion above this is not a realistic single serving
    private static final double MAX_SUGGESTED_GRAMS = 150;

    private final MealDAO mealDao = new MealDAOImpl();
    private final WaterDAO waterDao = new WaterDAOImpl();
    private final GoalDAO goalDao = new GoalDAOImpl();
    private final FoodDAO foodDao = new FoodDAOImpl();

    // ------------------------------------------------------------------
    // Daily totals and goals
    // ------------------------------------------------------------------

    /** Food totals and water total of ONE day (all zero if nothing was logged). */
    public DailySummary getDailySummary(int userId, LocalDate date) throws DatabaseException {
        List<DailySummary> totals = mealDao.findDailyTotals(userId, date, date);

        DailySummary summary;
        if (totals.isEmpty()) {
            summary = new DailySummary(date);
        } else {
            summary = totals.get(0);
        }
        summary.setWaterMl(waterDao.getTotalForDate(userId, date));
        return summary;
    }

    /** The user's saved goals, or sensible general goals if they have none yet. */
    public NutritionGoal getGoals(int userId) throws DatabaseException {
        NutritionGoal goal = goalDao.findByUser(userId);
        if (goal == null) {
            goal = new NutritionGoal(0, userId, 2000, 100, 250, 65, 2500);
        }
        return goal;
    }

    /**
     * Calories per meal type for one day's meals, for example
     * {Breakfast=512, Lunch=658, Snack=0, Dinner=0}.
     * LinkedHashMap keeps the keys in the order we put them in,
     * so they always come out as Breakfast, Lunch, Snack, Dinner.
     */
    public Map<String, Double> getCaloriesByMealType(List<Meal> meals) {
        Map<String, Double> totals = new LinkedHashMap<String, Double>();
        for (String mealType : MEAL_TYPES) {
            totals.put(mealType, 0.0);
        }
        for (Meal meal : meals) {
            Double soFar = totals.get(meal.getMealType());
            if (soFar == null) {
                soFar = 0.0; // a meal type that is not in our list
            }
            totals.put(meal.getMealType(), soFar + meal.getTotalCalories());
        }
        return totals;
    }

    // ------------------------------------------------------------------
    // Weekly report
    // ------------------------------------------------------------------

    /**
     * One entry per day for the last 7 days, oldest first, today last.
     * The database only returns days that have meals, so we first fill the
     * map with empty days and then replace the ones that have data.
     */
    public Map<LocalDate, DailySummary> getLastSevenDays(int userId) throws DatabaseException {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(6);

        Map<LocalDate, DailySummary> week = new LinkedHashMap<LocalDate, DailySummary>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = firstDay.plusDays(i);
            week.put(day, new DailySummary(day));
        }

        for (DailySummary summary : mealDao.findDailyTotals(userId, firstDay, today)) {
            week.put(summary.getDate(), summary);
        }
        return week;
    }

    /** Average calories over the days that have meals (empty days are skipped). */
    public double getAverageCalories(Map<LocalDate, DailySummary> week) {
        double total = 0;
        int loggedDays = 0;
        for (DailySummary day : week.values()) {
            if (day.hasMeals()) {
                total += day.getCalories();
                loggedDays++;
            }
        }
        if (loggedDays == 0) {
            return 0;
        }
        return total / loggedDays;
    }

    /** The day whose calories were closest to the goal, or null if nothing was logged. */
    public DailySummary getBestDay(Map<LocalDate, DailySummary> week, int goalCalories) {
        DailySummary best = null;
        double smallestDistance = Double.MAX_VALUE;

        for (DailySummary day : week.values()) {
            if (!day.hasMeals()) {
                continue;
            }
            double distance = Math.abs(day.getCalories() - goalCalories);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                best = day;
            }
        }
        return best;
    }

    /**
     * Number of days in a row (ending today) with at least one meal logged.
     * If today has nothing yet we start counting from yesterday, so the
     * streak is not shown as broken just because it is still morning.
     */
    public int getLoggingStreak(int userId) throws DatabaseException {
        // A HashSet answers "was something logged on this date?" instantly
        Set<LocalDate> loggedDates = new HashSet<LocalDate>(mealDao.findLoggedDates(userId));

        LocalDate day = LocalDate.now();
        if (!loggedDates.contains(day)) {
            day = day.minusDays(1);
        }

        int streak = 0;
        while (loggedDates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    // ------------------------------------------------------------------
    // "What should I eat next?"
    // ------------------------------------------------------------------

    /**
     * Finds which of calories / protein / carbs / fat is furthest from its goal today.
     * We compare the PERCENTAGE still missing, because kcal and grams cannot be
     * compared directly. Returns null when every goal has been reached.
     */
    public String findBiggestGap(DailySummary today, NutritionGoal goal) {
        Map<String, Double> missingPercent = new LinkedHashMap<String, Double>();
        missingPercent.put(CALORIES, percentMissing(today.getCalories(), goal.getTargetCalories()));
        missingPercent.put(PROTEIN, percentMissing(today.getProtein(), goal.getTargetProtein()));
        missingPercent.put(CARBS, percentMissing(today.getCarbs(), goal.getTargetCarbs()));
        missingPercent.put(FAT, percentMissing(today.getFat(), goal.getTargetFat()));

        String biggest = null;
        double biggestPercent = 0;
        for (Map.Entry<String, Double> entry : missingPercent.entrySet()) {
            if (entry.getValue() > biggestPercent) {
                biggestPercent = entry.getValue();
                biggest = entry.getKey();
            }
        }
        return biggest;
    }

    /**
     * The top 3 foods in the library that are richest in the given nutrient,
     * each with the grams needed to fill today's gap (at most 150 g).
     */
    public List<FoodSuggestion> suggestFoods(final String nutrient, DailySummary today, NutritionGoal goal)
            throws DatabaseException {
        double remaining = getTarget(goal, nutrient) - getConsumed(today, nutrient);
        List<FoodSuggestion> suggestions = new ArrayList<FoodSuggestion>();
        if (remaining <= 0) {
            return suggestions;
        }

        // Rank the whole library: the food with the MOST of this nutrient per 100 g comes first
        List<Food> foods = foodDao.findAll();
        Collections.sort(foods, new Comparator<Food>() {
            @Override
            public int compare(Food first, Food second) {
                // second before first = descending order
                return Double.compare(getPer100g(second, nutrient), getPer100g(first, nutrient));
            }
        });

        for (Food food : foods) {
            if (suggestions.size() == SUGGESTION_COUNT) {
                break;
            }
            double per100g = getPer100g(food, nutrient);
            if (per100g <= 0) {
                continue; // this food has none of the nutrient
            }

            // grams needed to fill the gap, rounded to the nearest 10 g, between 10 g and 150 g
            double grams = remaining / per100g * 100;
            grams = Math.round(grams / 10) * 10;
            grams = Math.max(10, Math.min(grams, MAX_SUGGESTED_GRAMS));

            suggestions.add(new FoodSuggestion(food, grams, per100g * grams / 100));
        }
        return suggestions;
    }

    /** "kcal" for calories, "g" for everything else. */
    public static String getUnit(String nutrient) {
        return CALORIES.equals(nutrient) ? "kcal" : "g";
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private double percentMissing(double consumed, double target) {
        if (target <= 0) {
            return 0; // no goal set, so nothing is "missing"
        }
        return (target - consumed) / target * 100;
    }

    private double getPer100g(Food food, String nutrient) {
        if (PROTEIN.equals(nutrient)) {
            return food.getProtein();
        } else if (CARBS.equals(nutrient)) {
            return food.getCarbs();
        } else if (FAT.equals(nutrient)) {
            return food.getFat();
        }
        return food.getCalories();
    }

    private double getConsumed(DailySummary summary, String nutrient) {
        if (PROTEIN.equals(nutrient)) {
            return summary.getProtein();
        } else if (CARBS.equals(nutrient)) {
            return summary.getCarbs();
        } else if (FAT.equals(nutrient)) {
            return summary.getFat();
        }
        return summary.getCalories();
    }

    private double getTarget(NutritionGoal goal, String nutrient) {
        if (PROTEIN.equals(nutrient)) {
            return goal.getTargetProtein();
        } else if (CARBS.equals(nutrient)) {
            return goal.getTargetCarbs();
        } else if (FAT.equals(nutrient)) {
            return goal.getTargetFat();
        }
        return goal.getTargetCalories();
    }
}
