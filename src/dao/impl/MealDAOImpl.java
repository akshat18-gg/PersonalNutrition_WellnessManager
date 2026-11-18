package dao.impl;

import dao.MealDAO;
import database.DBConnection;
import exception.DatabaseException;
import model.DailySummary;
import model.Food;
import model.Meal;
import model.MealItem;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of MealDAO for the meals and meal_items tables.
 *
 * addItem and deleteItem each need more than one SQL statement, so they run
 * inside a TRANSACTION: either every step is saved, or none of them is.
 */
public class MealDAOImpl implements MealDAO {

    @Override
    public int addItem(int userId, String mealType, LocalDate date, int foodId, double quantity)
            throws DatabaseException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // start the transaction

            // Reuse this day's meal of the same type if there is one
            int mealId = findMealId(conn, userId, mealType, date);
            if (mealId == -1) {
                mealId = insertMeal(conn, userId, mealType, date);
            }
            int mealItemId = insertItem(conn, mealId, foodId, quantity);

            conn.commit(); // both steps worked, make them permanent
            return mealItemId;

        } catch (SQLException e) {
            rollback(conn); // undo the half-finished work
            throw new DatabaseException("Could not save the meal.", e);
        } finally {
            // finally runs on success AND on failure, so the connection is always closed
            close(conn);
        }
    }

    @Override
    public void deleteItem(int mealItemId) throws DatabaseException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int mealId = findMealIdOfItem(conn, mealItemId);
            runUpdate(conn, "DELETE FROM meal_items WHERE meal_item_id = ?", mealItemId);

            // An empty meal is useless, so remove it as well
            if (mealId != -1 && countItems(conn, mealId) == 0) {
                runUpdate(conn, "DELETE FROM meals WHERE meal_id = ?", mealId);
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            throw new DatabaseException("Could not delete the meal item.", e);
        } finally {
            close(conn);
        }
    }

    @Override
    public List<Meal> findByDate(int userId, LocalDate date) throws DatabaseException {
        // FIELD(...) sorts the meals in the order of the day instead of alphabetically
        String sql = "SELECT m.meal_id, m.meal_type, m.meal_date, mi.meal_item_id, mi.quantity, "
                   + "f.food_id, f.food_name, f.category, f.calories, f.protein, f.carbs, f.fat "
                   + "FROM meals m "
                   + "JOIN meal_items mi ON m.meal_id = mi.meal_id "
                   + "JOIN food f ON mi.food_id = f.food_id "
                   + "WHERE m.user_id = ? AND m.meal_date = ? "
                   + "ORDER BY FIELD(m.meal_type, 'Breakfast', 'Lunch', 'Snack', 'Dinner'), mi.meal_item_id";

        // meal_id -> Meal. The query returns one row per ITEM, so the same meal
        // appears on several rows; the map lets us find the Meal we already made.
        Map<Integer, Meal> mealsById = new LinkedHashMap<Integer, Meal>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int mealId = rs.getInt("meal_id");
                    Meal meal = mealsById.get(mealId);
                    if (meal == null) {
                        meal = new Meal(mealId, userId, rs.getString("meal_type"),
                                rs.getDate("meal_date").toLocalDate());
                        mealsById.put(mealId, meal);
                    }
                    meal.addItem(mapItem(rs, mealId));
                }
            }
            return new ArrayList<Meal>(mealsById.values());

        } catch (SQLException e) {
            throw new DatabaseException("Could not load the meals.", e);
        }
    }

    @Override
    public List<DailySummary> findDailyTotals(int userId, LocalDate from, LocalDate to) throws DatabaseException {
        // Food values are per 100 g, so each item contributes value * quantity / 100
        String sql = "SELECT m.meal_date, "
                   + "SUM(f.calories * mi.quantity / 100) AS total_calories, "
                   + "SUM(f.protein * mi.quantity / 100) AS total_protein, "
                   + "SUM(f.carbs * mi.quantity / 100) AS total_carbs, "
                   + "SUM(f.fat * mi.quantity / 100) AS total_fat "
                   + "FROM meals m "
                   + "JOIN meal_items mi ON m.meal_id = mi.meal_id "
                   + "JOIN food f ON mi.food_id = f.food_id "
                   + "WHERE m.user_id = ? AND m.meal_date BETWEEN ? AND ? "
                   + "GROUP BY m.meal_date ORDER BY m.meal_date";

        List<DailySummary> totals = new ArrayList<DailySummary>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    totals.add(new DailySummary(
                            rs.getDate("meal_date").toLocalDate(),
                            rs.getDouble("total_calories"),
                            rs.getDouble("total_protein"),
                            rs.getDouble("total_carbs"),
                            rs.getDouble("total_fat")));
                }
            }
            return totals;

        } catch (SQLException e) {
            throw new DatabaseException("Could not calculate the daily totals.", e);
        }
    }

    @Override
    public List<LocalDate> findLoggedDates(int userId) throws DatabaseException {
        String sql = "SELECT DISTINCT m.meal_date FROM meals m "
                   + "JOIN meal_items mi ON m.meal_id = mi.meal_id "
                   + "WHERE m.user_id = ? ORDER BY m.meal_date DESC";

        List<LocalDate> dates = new ArrayList<LocalDate>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getDate("meal_date").toLocalDate());
                }
            }
            return dates;

        } catch (SQLException e) {
            throw new DatabaseException("Could not load the logging history.", e);
        }
    }

    // ---------- small helpers used inside the transactions ----------
    // They take the open Connection so that all steps share ONE transaction,
    // and they let SQLException pass up to the public method, which handles it.

    /** Returns the meal_id of the user's meal of this type on this date, or -1 if there is none. */
    private int findMealId(Connection conn, int userId, String mealType, LocalDate date) throws SQLException {
        String sql = "SELECT meal_id FROM meals WHERE user_id = ? AND meal_type = ? AND meal_date = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, mealType);
            stmt.setDate(3, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("meal_id") : -1;
            }
        }
    }

    private int insertMeal(Connection conn, int userId, String mealType, LocalDate date) throws SQLException {
        String sql = "INSERT INTO meals (user_id, meal_type, meal_date) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, mealType);
            stmt.setDate(3, Date.valueOf(date));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertItem(Connection conn, int mealId, int foodId, double quantity) throws SQLException {
        String sql = "INSERT INTO meal_items (meal_id, food_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, mealId);
            stmt.setInt(2, foodId);
            stmt.setDouble(3, quantity);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Returns the meal that an item belongs to, or -1 if the item does not exist. */
    private int findMealIdOfItem(Connection conn, int mealItemId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT meal_id FROM meal_items WHERE meal_item_id = ?")) {
            stmt.setInt(1, mealItemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("meal_id") : -1;
            }
        }
    }

    private int countItems(Connection conn, int mealId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM meal_items WHERE meal_id = ?")) {
            stmt.setInt(1, mealId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Runs a DELETE that has exactly one int parameter. */
    private void runUpdate(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private MealItem mapItem(ResultSet rs, int mealId) throws SQLException {
        Food food = new Food(
                rs.getInt("food_id"),
                rs.getString("food_name"),
                rs.getString("category"),
                rs.getDouble("calories"),
                rs.getDouble("protein"),
                rs.getDouble("carbs"),
                rs.getDouble("fat"));

        MealItem item = new MealItem(rs.getInt("meal_item_id"), mealId, food.getFoodId(), rs.getDouble("quantity"));
        item.setFood(food);
        return item;
    }

    private void rollback(Connection conn) {
        if (conn == null) {
            return; // the connection was never opened, nothing to undo
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("Rollback failed: " + e.getMessage());
        }
    }

    private void close(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println("Could not close the connection: " + e.getMessage());
        }
    }
}
