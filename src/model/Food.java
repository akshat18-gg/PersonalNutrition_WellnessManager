package model;

/**
 * Represents a food item in the food table.
 * Calories, protein, carbs and fat are all stored per 100 g of the food.
 */
public class Food {
    private int foodId;
    private String foodName;
    private String category;
    private double calories;
    private double protein;
    private double carbs;
    private double fat;

    public Food() {
    }

    public Food(int foodId, String foodName, String category, double calories, double protein, double carbs, double fat) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.category = category;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFat() {
        return fat;
    }

    public void setFat(double fat) {
        this.fat = fat;
    }

    // A JComboBox shows toString() of each item, so the Meals screen can hold
    // real Food objects in its drop-down and still display just the name.
    @Override
    public String toString() {
        return foodName;
    }
}
