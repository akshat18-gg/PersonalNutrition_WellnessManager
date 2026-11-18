package model;

/**
 * Represents daily nutrition targets in the nutrition_goals table.
 */
public class NutritionGoal {
    private int goalId;
    private int userId;
    private int targetCalories;
    private double targetProtein;
    private double targetCarbs;
    private double targetFat;
    private int targetWater;

    public NutritionGoal() {
    }

    public NutritionGoal(int goalId, int userId, int targetCalories, double targetProtein,
                         double targetCarbs, double targetFat, int targetWater) {
        this.goalId = goalId;
        this.userId = userId;
        this.targetCalories = targetCalories;
        this.targetProtein = targetProtein;
        this.targetCarbs = targetCarbs;
        this.targetFat = targetFat;
        this.targetWater = targetWater;
    }

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(int targetCalories) {
        this.targetCalories = targetCalories;
    }

    public double getTargetProtein() {
        return targetProtein;
    }

    public void setTargetProtein(double targetProtein) {
        this.targetProtein = targetProtein;
    }

    public double getTargetCarbs() {
        return targetCarbs;
    }

    public void setTargetCarbs(double targetCarbs) {
        this.targetCarbs = targetCarbs;
    }

    public double getTargetFat() {
        return targetFat;
    }

    public void setTargetFat(double targetFat) {
        this.targetFat = targetFat;
    }

    public int getTargetWater() {
        return targetWater;
    }

    public void setTargetWater(int targetWater) {
        this.targetWater = targetWater;
    }
}
