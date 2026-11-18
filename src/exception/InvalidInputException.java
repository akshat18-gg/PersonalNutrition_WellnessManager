package exception;

/**
 * Thrown by util.Validator when the user typed something we cannot accept
 * (empty text, not a number, negative or unrealistic value).
 */
public class InvalidInputException extends NutriTrackException {

    public InvalidInputException(String message) {
        super(message);
    }
}
