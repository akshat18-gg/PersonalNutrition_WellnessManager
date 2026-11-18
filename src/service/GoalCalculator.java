package service;

import model.NutritionGoal;
import model.User;

/**
 * The maths behind the Profile screen: BMI, BMR and suggested daily goals.
 * Pure calculations only (no database, no GUI), so every method is static.
 */
public final class GoalCalculator {

    public static final String[] GENDERS = {"Male", "Female", "Other"};
    public static final String[] ACTIVITY_LEVELS = {"Sedentary", "Light", "Moderate", "Active", "Very Active"};

    public static final String GOAL_LOSE = "Lose weight";
    public static final String GOAL_MAINTAIN = "Maintain weight";
    public static final String GOAL_GAIN = "Gain weight";
    public static final String[] GOAL_TYPES = {GOAL_LOSE, GOAL_MAINTAIN, GOAL_GAIN};

    // Eating 500 kcal less (or more) per day changes weight by roughly 0.5 kg per week
    private static final int CALORIE_ADJUSTMENT = 500;
    // Going below this is not considered safe without a doctor
    private static final int MINIMUM_CALORIES = 1200;

    private GoalCalculator() {
        // only static methods, no objects needed
    }

    /**
     * BMI = weight (kg) / height (m) squared
     */
    public static double calculateBmi(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    /** Standard WHO ranges. */
    public static String getBmiCategory(double bmi) {
        if (bmi < 18.5) {
            return "Underweight";
        } else if (bmi < 25) {
            return "Normal";
        } else if (bmi < 30) {
            return "Overweight";
        }
        return "Obese";
    }

    /**
     * BMR = calories the body burns in a day at complete rest.
     * Mifflin-St Jeor equation:
     *   BMR = 10 x weight(kg) + 6.25 x height(cm) - 5 x age + s
     *   s = +5 for men, -161 for women
     */
    public static double calculateBmr(User user) {
        double base = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge();
        if ("Male".equals(user.getGender())) {
            return base + 5;
        } else if ("Female".equals(user.getGender())) {
            return base - 161;
        }
        return base - 78; // "Other": the middle of the two values
    }

    /**
     * How much more than BMR a person burns because of daily movement.
     * These are the standard multipliers used with the Mifflin-St Jeor equation.
     */
    public static double getActivityFactor(String activityLevel) {
        if (activityLevel == null) {
            return 1.2;
        }
        switch (activityLevel) {
            case "Light":
                return 1.375;  // light exercise 1-3 days a week
            case "Moderate":
                return 1.55;   // exercise 3-5 days a week
            case "Active":
                return 1.725;  // hard exercise 6-7 days a week
            case "Very Active":
                return 1.9;    // physical job or training twice a day
            default:
                return 1.2;    // Sedentary: little or no exercise
        }
    }

    /**
     * Calories needed per day to stay at the same weight.
     *   Daily calories = BMR x activity factor
     */
    public static double calculateDailyCalories(User user) {
        return calculateBmr(user) * getActivityFactor(user.getActivityLevel());
    }

    /**
     * Suggests daily goals for this user.
     *   Calories : daily calories, -500 to lose weight, +500 to gain, never below 1200
     *   Protein  : 25% of calories / 4   (1 g protein = 4 kcal)
     *   Carbs    : 50% of calories / 4   (1 g carbs   = 4 kcal)
     *   Fat      : 25% of calories / 9   (1 g fat     = 9 kcal)
     *   Water    : 35 ml per kg of body weight
     */
    public static NutritionGoal suggestGoals(User user, String goalType) {
        double calories = calculateDailyCalories(user);
        if (GOAL_LOSE.equals(goalType)) {
            calories -= CALORIE_ADJUSTMENT;
        } else if (GOAL_GAIN.equals(goalType)) {
            calories += CALORIE_ADJUSTMENT;
        }
        calories = Math.max(calories, MINIMUM_CALORIES);

        NutritionGoal goal = new NutritionGoal();
        goal.setUserId(user.getUserId());
        goal.setTargetCalories(roundTo(calories, 10));
        goal.setTargetProtein(Math.round(calories * 0.25 / 4));
        goal.setTargetCarbs(Math.round(calories * 0.50 / 4));
        goal.setTargetFat(Math.round(calories * 0.25 / 9));
        goal.setTargetWater(roundTo(user.getWeight() * 35, 50));
        return goal;
    }

    // Rounds to the nearest step so goals are friendly numbers (2610, not 2609.81)
    private static int roundTo(double value, int step) {
        return (int) (Math.round(value / step) * step);
    }
}
