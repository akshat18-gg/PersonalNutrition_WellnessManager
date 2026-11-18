import database.DBConnection;
import gui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Main class for NutriTrack.
 * 1. Tests the JDBC database connection.
 * 2. Launches the Login screen.
 */
public class Main {

    public static void main(String[] args) {
        // Swing windows must be created on Swing's own thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                checkDatabase();

                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }

    /** Warns the user straight away if MySQL cannot be reached, instead of failing at login. */
    private static void checkDatabase() {
        // try-with-resources closes the test connection automatically
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Connected to database: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "NutriTrack could not connect to the database.\n\n"
                    + "1. Check that MySQL is running.\n"
                    + "2. Check the username and password in config/db.properties.\n"
                    + "3. Check that sql/nutritrack_schema.sql has been run.",
                    "Database not available", JOptionPane.ERROR_MESSAGE);
        }
    }
}
