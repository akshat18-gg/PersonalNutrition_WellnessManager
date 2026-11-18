package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to the MySQL database 'nutritrack'.
 *
 * The URL, username and password are read from config/db.properties, so each
 * teammate can set their own MySQL password without editing Java code.
 * If the file is missing, the default values below are used.
 */
public class DBConnection {

    private static final String CONFIG_FILE = "config/db.properties";

    // Used only when a value is not found in the config file
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/nutritrack";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "mysql1234";

    // Loaded once when the class is first used
    private static final Properties CONFIG = loadConfig();

    private static Properties loadConfig() {
        Properties properties = new Properties();
        // try-with-resources: the file is closed automatically, even if load() fails
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // Not an error: the app still works with the defaults
            System.out.println("Could not read " + CONFIG_FILE + ", using default database settings.");
        }
        return properties;
    }

    /**
     * Establishes and returns a Connection to the MySQL database.
     * The caller must close it (the DAO classes do this with try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        String url = CONFIG.getProperty("db.url", DEFAULT_URL);
        String user = CONFIG.getProperty("db.user", DEFAULT_USER);
        String password = CONFIG.getProperty("db.password", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }
}
