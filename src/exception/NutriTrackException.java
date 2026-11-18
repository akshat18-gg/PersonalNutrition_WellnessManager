package exception;

/**
 * Base class for every error that NutriTrack itself reports.
 *
 * It is a CHECKED exception (extends Exception, not RuntimeException), so the
 * compiler forces the calling code to either catch it or declare "throws".
 * The GUI can catch this one type to handle any problem from the lower layers.
 */
public class NutriTrackException extends Exception {

    public NutriTrackException(String message) {
        super(message);
    }

    // The "cause" keeps the original low-level exception for debugging
    public NutriTrackException(String message, Throwable cause) {
        super(message, cause);
    }
}
