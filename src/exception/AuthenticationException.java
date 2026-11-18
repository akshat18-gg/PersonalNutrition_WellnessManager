package exception;

/**
 * Thrown by service.AuthService when the email is not registered
 * or the password does not match.
 */
public class AuthenticationException extends NutriTrackException {

    public AuthenticationException(String message) {
        super(message);
    }
}
