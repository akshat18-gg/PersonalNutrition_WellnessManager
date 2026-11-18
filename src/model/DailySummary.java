package model;

import java.time.LocalDate;

/**
 * Everything the user consumed on one day, added up.
 * Not a database table: it is the result of SUM queries on meals and water.
 * Used by the dashboard, the weekly report and the report exporters.
 */
public class DailySummary {
    private LocalDate date;
    private double calories;
    private double protein;
    private double carbs;
    private double fat;
    private int waterMl;

    /** A summary with all totals at zero (a day with nothing logged). */
    public DailySummary(LocalDate date) {
        this.date = date;
    }

    public DailySummary(LocalDate date, double calories, double protein, double carbs, double fat) {
        this.date = date;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getCalories() {
        return calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getFat() {
        return fat;
    }

    public int getWaterMl() {
        return waterMl;
    }

    public void setWaterMl(int waterMl) {
        this.waterMl = waterMl;
    }

    /** True when at least one meal was logged on this day. */
    public boolean hasMeals() {
        return calories > 0;
    }
}
