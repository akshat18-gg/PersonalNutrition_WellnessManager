package exception;

/**
 * Thrown by the DAO classes when a JDBC call fails.
 *
 * It WRAPS the original SQLException as its cause. The message is simple
 * text that is safe to show to the user, while the cause keeps the real
 * SQL error for the developer (see UIStyles.showError, which prints it).
 */
public class DatabaseException extends NutriTrackException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
