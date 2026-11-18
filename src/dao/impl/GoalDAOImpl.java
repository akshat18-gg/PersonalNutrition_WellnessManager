package dao.impl;

import dao.GoalDAO;
import database.DBConnection;
import exception.DatabaseException;
import model.NutritionGoal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC implementation of GoalDAO for the nutrition_goals table.
 */
public class GoalDAOImpl implements GoalDAO {

    @Override
    public NutritionGoal findByUser(int userId) throws DatabaseException {
        String sql = "SELECT goal_id, user_id, target_calories, target_protein, target_carbs, target_fat, target_water "
                   + "FROM nutrition_goals WHERE user_id = ? ORDER BY goal_id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new NutritionGoal(
                        rs.getInt("goal_id"),
                        rs.getInt("user_id"),
                        rs.getInt("target_calories"),
                        rs.getDouble("target_protein"),
                        rs.getDouble("target_carbs"),
                        rs.getDouble("target_fat"),
                        rs.getInt("target_water"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not load your goals.", e);
        }
    }

    @Override
    public void save(NutritionGoal goal) throws DatabaseException {
        // Each user keeps one row of goals: update it if it exists, otherwise insert it.
        // Both statements take the parameters in the same order, so the code below is shared.
        String sql;
        if (findByUser(goal.getUserId()) != null) {
            sql = "UPDATE nutrition_goals SET target_calories = ?, target_protein = ?, target_carbs = ?, "
                + "target_fat = ?, target_water = ? WHERE user_id = ?";
        } else {
            sql = "INSERT INTO nutrition_goals (target_calories, target_protein, target_carbs, target_fat, "
                + "target_water, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goal.getTargetCalories());
            stmt.setDouble(2, goal.getTargetProtein());
            stmt.setDouble(3, goal.getTargetCarbs());
            stmt.setDouble(4, goal.getTargetFat());
            stmt.setInt(5, goal.getTargetWater());
            stmt.setInt(6, goal.getUserId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save your goals.", e);
        }
    }
}
