package model;

/**
 * One line of the "What should I eat next?" card:
 * a food, how many grams to eat, and how much of the missing nutrient that gives.
 */
public class FoodSuggestion {
    private Food food;
    private double grams;
    private double nutrientAmount;

    public FoodSuggestion(Food food, double grams, double nutrientAmount) {
        this.food = food;
        this.grams = grams;
        this.nutrientAmount = nutrientAmount;
    }

    public Food getFood() {
        return food;
    }

    public double getGrams() {
        return grams;
    }

    public double getNutrientAmount() {
        return nutrientAmount;
    }
}
