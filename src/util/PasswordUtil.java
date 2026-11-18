package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns a password into a SHA-256 hash so the real password is never stored.
 *
 * The result is 64 lowercase hex characters, the same format that MySQL's
 * SHA2('text', 256) produces. That is why the demo user in the SQL script
 * can log in from Java.
 */
public final class PasswordUtil {

    private PasswordUtil() {
        // only static methods, no objects needed
    }

    public static String hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Convert each byte to two hex characters (for example 10 -> "0a")
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // Every JDK must include SHA-256, so this should never happen.
            // We rethrow as an unchecked exception and keep the cause.
            throw new IllegalStateException("SHA-256 is not available in this Java installation", e);
        }
    }

    /** Hashes the typed password and compares it with the hash stored in the database. */
    public static boolean matches(String plainPassword, String storedHash) {
        return hash(plainPassword).equalsIgnoreCase(storedHash);
    }
}
