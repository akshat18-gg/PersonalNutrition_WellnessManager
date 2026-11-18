package dao;

import exception.DatabaseException;
import model.NutritionGoal;

/**
 * Everything the app needs to do with the nutrition_goals table.
 */
public interface GoalDAO {

    /** Returns the user's saved goals, or null if they have not saved any yet. */
    NutritionGoal findByUser(int userId) throws DatabaseException;

    /** Updates the user's goals, or inserts them if this is the first time. */
    void save(NutritionGoal goal) throws DatabaseException;
}
