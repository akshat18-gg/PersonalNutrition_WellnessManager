package dao.impl;

import dao.WaterDAO;
import database.DBConnection;
import exception.DatabaseException;
import model.WaterIntake;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of WaterDAO for the water_intake table.
 */
public class WaterDAOImpl implements WaterDAO {

    @Override
    public int insert(WaterIntake entry) throws DatabaseException {
        String sql = "INSERT INTO water_intake (user_id, amount_ml, recorded_at) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, entry.getUserId());
            stmt.setInt(2, entry.getAmountMl());
            stmt.setTimestamp(3, Timestamp.valueOf(entry.getRecordedAt()));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not save the water entry.", e);
        }
    }

    @Override
    public List<WaterIntake> findByDate(int userId, LocalDate date) throws DatabaseException {
        // DATE(recorded_at) cuts the time off the timestamp so it can be compared with a date
        String sql = "SELECT water_id, user_id, amount_ml, recorded_at FROM water_intake "
                   + "WHERE user_id = ? AND DATE(recorded_at) = ? ORDER BY recorded_at DESC, water_id DESC";

        List<WaterIntake> entries = new ArrayList<WaterIntake>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new WaterIntake(
                            rs.getInt("water_id"),
                            rs.getInt("user_id"),
                            rs.getInt("amount_ml"),
                            rs.getTimestamp("recorded_at").toLocalDateTime()));
                }
            }
            return entries;

        } catch (SQLException e) {
            throw new DatabaseException("Could not load the water entries.", e);
        }
    }

    @Override
    public int getTotalForDate(int userId, LocalDate date) throws DatabaseException {
        // IFNULL turns "no rows" (NULL) into 0
        String sql = "SELECT IFNULL(SUM(amount_ml), 0) FROM water_intake WHERE user_id = ? AND DATE(recorded_at) = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not calculate the water total.", e);
        }
    }

    @Override
    public void delete(int waterId) throws DatabaseException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM water_intake WHERE water_id = ?")) {

            stmt.setInt(1, waterId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the water entry.", e);
        }
    }
}
