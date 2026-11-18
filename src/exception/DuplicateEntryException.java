package exception;

/**
 * Thrown when the user tries to save something that already exists,
 * for example signing up with an email that is already registered
 * or adding a food name that is already in the library.
 */
public class DuplicateEntryException extends NutriTrackException {

    public DuplicateEntryException(String message) {
        super(message);
    }

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
