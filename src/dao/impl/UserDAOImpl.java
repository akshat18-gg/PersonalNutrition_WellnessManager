package dao.impl;

import dao.UserDAO;
import database.DBConnection;
import exception.DatabaseException;
import exception.DuplicateEntryException;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/**
 * JDBC implementation of UserDAO for the users table.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByEmail(String email) throws DatabaseException {
        String sql = "SELECT user_id, name, email, password, age, gender, height, weight, activity_level "
                   + "FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not look up the user account.", e);
        }
    }

    @Override
    public int insert(User user) throws DuplicateEntryException, DatabaseException {
        String sql = "INSERT INTO users (name, email, password, age, gender, height, weight, activity_level) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setInt(4, user.getAge());
            stmt.setString(5, user.getGender());
            stmt.setDouble(6, user.getHeight());
            stmt.setDouble(7, user.getWeight());
            stmt.setString(8, user.getActivityLevel());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            // The email column is UNIQUE, so MySQL rejects a second account with the same email.
            // This catch must come BEFORE SQLException because it is a subclass of it.
            throw new DuplicateEntryException("An account with this email already exists.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Could not create the account.", e);
        }
    }

    @Override
    public void updateProfile(User user) throws DatabaseException {
        String sql = "UPDATE users SET name = ?, age = ?, gender = ?, height = ?, weight = ?, activity_level = ? "
                   + "WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setInt(2, user.getAge());
            stmt.setString(3, user.getGender());
            stmt.setDouble(4, user.getHeight());
            stmt.setDouble(5, user.getWeight());
            stmt.setString(6, user.getActivityLevel());
            stmt.setInt(7, user.getUserId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save the profile.", e);
        }
    }

    @Override
    public void delete(int userId) throws DatabaseException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the account.", e);
        }
    }

    // Turns the current row of a ResultSet into a User object
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setAge(rs.getInt("age"));
        user.setGender(rs.getString("gender"));
        user.setHeight(rs.getDouble("height"));
        user.setWeight(rs.getDouble("weight"));
        user.setActivityLevel(rs.getString("activity_level"));
        return user;
    }
}
