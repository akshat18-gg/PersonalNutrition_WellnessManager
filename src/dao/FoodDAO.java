package dao;

import exception.DatabaseException;
import exception.DuplicateEntryException;
import model.Food;

import java.util.List;

/**
 * Everything the app needs to do with the food table (the Food Library).
 */
public interface FoodDAO {

    /** All foods, sorted by name. */
    List<Food> findAll() throws DatabaseException;

    /** Returns the food with this exact name, or null if there is none. */
    Food findByName(String foodName) throws DatabaseException;

    /** Saves a new food and returns the generated food_id. */
    int insert(Food food) throws DuplicateEntryException, DatabaseException;

    void update(Food food) throws DuplicateEntryException, DatabaseException;

    void delete(int foodId) throws DatabaseException;

    /** How many logged meal items use this food (shown as a warning before deleting). */
    int countUsages(int foodId) throws DatabaseException;
}
