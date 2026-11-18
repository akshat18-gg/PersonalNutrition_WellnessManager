package dao;

import exception.DatabaseException;
import model.DailySummary;
import model.Meal;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the app needs to do with the meals and meal_items tables.
 */
public interface MealDAO {

    /**
     * Adds one food to the user's meal of this type on this date.
     * If that meal does not exist yet it is created first, so there is only
     * ever ONE Breakfast / Lunch / Snack / Dinner per day.
     * Returns the new meal_item_id.
     */
    int addItem(int userId, String mealType, LocalDate date, int foodId, double quantity)
            throws DatabaseException;

    /** The user's meals on one day, each with its items and their foods filled in. */
    List<Meal> findByDate(int userId, LocalDate date) throws DatabaseException;

    /** Deletes one item. If its meal has no items left, the meal is deleted too. */
    void deleteItem(int mealItemId) throws DatabaseException;

    /**
     * Calories, protein, carbs and fat added up per day, for every day between
     * 'from' and 'to' on which something was logged. Days with no meals are not in the list.
     */
    List<DailySummary> findDailyTotals(int userId, LocalDate from, LocalDate to) throws DatabaseException;

    /** Every date on which the user logged at least one meal item, newest first. */
    List<LocalDate> findLoggedDates(int userId) throws DatabaseException;
}
