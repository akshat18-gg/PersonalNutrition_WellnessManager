package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a meal entry in the meals table (for example "Lunch on 17 Nov").
 * A meal HAS many items, so it keeps a list of MealItem objects.
 */
public class Meal {
    private int mealId;
    private int userId;
    private String mealType;
    private LocalDate mealDate;
    private List<MealItem> items = new ArrayList<MealItem>();

    public Meal() {
    }

    public Meal(int mealId, int userId, String mealType, LocalDate mealDate) {
        this.mealId = mealId;
        this.userId = userId;
        this.mealType = mealType;
        this.mealDate = mealDate;
    }

    public int getMealId() {
        return mealId;
    }

    public void setMealId(int mealId) {
        this.mealId = mealId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public LocalDate getMealDate() {
        return mealDate;
    }

    public void setMealDate(LocalDate mealDate) {
        this.mealDate = mealDate;
    }

    public List<MealItem> getItems() {
        return items;
    }

    public void addItem(MealItem item) {
        items.add(item);
    }

    /** Calories of the whole meal = sum of the calories of its items. */
    public double getTotalCalories() {
        double total = 0;
        for (MealItem item : items) {
            total += item.getCalories();
        }
        return total;
    }
}
