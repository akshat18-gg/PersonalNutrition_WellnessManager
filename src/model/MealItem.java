package model;

/**
 * Represents an individual food item entry inside a meal (meal_items table).
 *
 * It also holds the Food object it points to, so the nutrition of this item
 * can be calculated here instead of repeating the formula on every screen.
 */
public class MealItem {
    private int mealItemId;
    private int mealId;
    private int foodId;
    private double quantity; // grams
    private Food food;

    public MealItem() {
    }

    public MealItem(int mealItemId, int mealId, int foodId, double quantity) {
        this.mealItemId = mealItemId;
        this.mealId = mealId;
        this.foodId = foodId;
        this.quantity = quantity;
    }

    public int getMealItemId() {
        return mealItemId;
    }

    public void setMealItemId(int mealItemId) {
        this.mealItemId = mealItemId;
    }

    public int getMealId() {
        return mealId;
    }

    public void setMealId(int mealId) {
        this.mealId = mealId;
    }

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    // Food values are stored per 100 g, so: value x quantity / 100

    public double getCalories() {
        return food.getCalories() * quantity / 100;
    }

    public double getProtein() {
        return food.getProtein() * quantity / 100;
    }

    public double getCarbs() {
        return food.getCarbs() * quantity / 100;
    }

    public double getFat() {
        return food.getFat() * quantity / 100;
    }
}
