package dao.impl;

import dao.FoodDAO;
import database.DBConnection;
import exception.DatabaseException;
import exception.DuplicateEntryException;
import model.Food;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of FoodDAO for the food table.
 */
public class FoodDAOImpl implements FoodDAO {

    private static final String SELECT_FOOD =
            "SELECT food_id, food_name, category, calories, protein, carbs, fat FROM food ";

    @Override
    public List<Food> findAll() throws DatabaseException {
        List<Food> foods = new ArrayList<Food>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_FOOD + "ORDER BY food_name");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                foods.add(mapRow(rs));
            }
            return foods;

        } catch (SQLException e) {
            throw new DatabaseException("Could not load the food library.", e);
        }
    }

    @Override
    public Food findByName(String foodName) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_FOOD + "WHERE food_name = ?")) {

            stmt.setString(1, foodName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not search the food library.", e);
        }
    }

    @Override
    public int insert(Food food) throws DuplicateEntryException, DatabaseException {
        // Two foods with the same name would be confusing in the Meals drop-down
        if (findByName(food.getFoodName()) != null) {
            throw new DuplicateEntryException("\"" + food.getFoodName() + "\" is already in the food library.");
        }

        String sql = "INSERT INTO food (food_name, category, calories, protein, carbs, fat) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            fillFoodValues(stmt, food);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not add the food.", e);
        }
    }

    @Override
    public void update(Food food) throws DuplicateEntryException, DatabaseException {
        // Renaming a food to a name that ANOTHER food already has is not allowed
        Food sameName = findByName(food.getFoodName());
        if (sameName != null && sameName.getFoodId() != food.getFoodId()) {
            throw new DuplicateEntryException("Another food is already called \"" + food.getFoodName() + "\".");
        }

        String sql = "UPDATE food SET food_name = ?, category = ?, calories = ?, protein = ?, carbs = ?, fat = ? "
                   + "WHERE food_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            fillFoodValues(stmt, food);
            stmt.setInt(7, food.getFoodId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not update the food.", e);
        }
    }

    @Override
    public void delete(int foodId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM food WHERE food_id = ?")) {

            stmt.setInt(1, foodId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the food.", e);
        }
    }

    @Override
    public int countUsages(int foodId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM meal_items WHERE food_id = ?")) {

            stmt.setInt(1, foodId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not check where the food is used.", e);
        }
    }

    // INSERT and UPDATE set the same first six parameters, so they share this method
    private void fillFoodValues(PreparedStatement stmt, Food food) throws SQLException {
        stmt.setString(1, food.getFoodName());
        stmt.setString(2, food.getCategory());
        stmt.setDouble(3, food.getCalories());
        stmt.setDouble(4, food.getProtein());
        stmt.setDouble(5, food.getCarbs());
        stmt.setDouble(6, food.getFat());
    }

    // Turns the current row of a ResultSet into a Food object
    private Food mapRow(ResultSet rs) throws SQLException {
        return new Food(
                rs.getInt("food_id"),
                rs.getString("food_name"),
                rs.getString("category"),
                rs.getDouble("calories"),
                rs.getDouble("protein"),
                rs.getDouble("carbs"),
                rs.getDouble("fat"));
    }
}
