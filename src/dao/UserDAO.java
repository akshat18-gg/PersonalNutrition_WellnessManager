package dao;

import exception.DatabaseException;
import exception.DuplicateEntryException;
import model.User;

/**
 * Everything the app needs to do with the users table.
 * The rest of the app uses this interface and does not know (or care)
 * that the real work is done with JDBC in dao.impl.UserDAOImpl.
 */
public interface UserDAO {

    /** Returns the user with this email, or null if nobody has registered with it. */
    User findByEmail(String email) throws DatabaseException;

    /** Saves a new user and returns the generated user_id. */
    int insert(User user) throws DuplicateEntryException, DatabaseException;

    /** Saves changed profile details (name, age, gender, height, weight, activity level). */
    void updateProfile(User user) throws DatabaseException;

    /** Removes a user. Their meals, water and goals go too (ON DELETE CASCADE). */
    void delete(int userId) throws DatabaseException;
}
