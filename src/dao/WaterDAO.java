package dao;

import exception.DatabaseException;
import model.WaterIntake;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the app needs to do with the water_intake table.
 */
public interface WaterDAO {

    /** Saves one water entry and returns the generated water_id. */
    int insert(WaterIntake entry) throws DatabaseException;

    /** The user's water entries on one day, newest first. */
    List<WaterIntake> findByDate(int userId, LocalDate date) throws DatabaseException;

    /** Total ml the user drank on one day (0 if nothing was logged). */
    int getTotalForDate(int userId, LocalDate date) throws DatabaseException;

    void delete(int waterId) throws DatabaseException;
}
