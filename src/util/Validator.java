package util;

import exception.InvalidInputException;

/**
 * Checks everything the user types before it reaches the database.
 *
 * Every method either returns a clean value or throws InvalidInputException
 * with a message that can be shown directly in a dialog. Keeping the rules
 * here means all screens validate in exactly the same way.
 */
public final class Validator {

    // Limits for one entry. Anything above these is almost surely a typing mistake.
    public static final double MAX_QUANTITY_GRAMS = 2000;
    public static final int MAX_WATER_ML = 5000;

    private Validator() {
        // only static methods, no objects needed
    }

    /** Returns the trimmed text, or throws if it is empty. */
    public static String requireText(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    /** Converts text to a decimal number, or throws if it is not a number. */
    public static double parseNumber(String text, String fieldName) throws InvalidInputException {
        String clean = requireText(text, fieldName);
        try {
            double value = Double.parseDouble(clean);
            // parseDouble accepts the words "NaN" and "Infinity", which are useless to us
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new InvalidInputException(fieldName + " must be a number.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a number (you typed \"" + clean + "\").");
        }
    }

    /** Converts text to a whole number, or throws if it is not one. */
    public static int parseWholeNumber(String text, String fieldName) throws InvalidInputException {
        String clean = requireText(text, fieldName);
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a whole number (you typed \"" + clean + "\").");
        }
    }

    /** Decimal number that must lie between min and max (both allowed). */
    public static double parseNumberInRange(String text, String fieldName, double min, double max)
            throws InvalidInputException {
        double value = parseNumber(text, fieldName);
        if (value < min || value > max) {
            throw new InvalidInputException(fieldName + " must be between " + trim(min) + " and " + trim(max) + ".");
        }
        return value;
    }

    /** Whole number that must lie between min and max (both allowed). */
    public static int parseWholeNumberInRange(String text, String fieldName, int min, int max)
            throws InvalidInputException {
        int value = parseWholeNumber(text, fieldName);
        if (value < min || value > max) {
            throw new InvalidInputException(fieldName + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** Food quantity in grams: more than 0 and not more than 2000 g in one entry. */
    public static double parseQuantity(String text) throws InvalidInputException {
        double grams = parseNumber(text, "Quantity");
        if (grams <= 0) {
            throw new InvalidInputException("Quantity must be greater than 0 g.");
        }
        if (grams > MAX_QUANTITY_GRAMS) {
            throw new InvalidInputException("Quantity cannot be more than " + trim(MAX_QUANTITY_GRAMS) + " g in one entry.");
        }
        return grams;
    }

    /** Water in ml: more than 0 and not more than 5000 ml in one entry. */
    public static int parseWaterAmount(String text) throws InvalidInputException {
        int ml = parseWholeNumber(text, "Water amount");
        if (ml <= 0) {
            throw new InvalidInputException("Water amount must be greater than 0 ml.");
        }
        if (ml > MAX_WATER_ML) {
            throw new InvalidInputException("Water amount cannot be more than " + MAX_WATER_ML + " ml in one entry.");
        }
        return ml;
    }

    /** A very simple email check: some text, an @, and a dot after it. */
    public static String requireEmail(String text) throws InvalidInputException {
        String email = requireText(text, "Email").toLowerCase();
        int at = email.indexOf('@');
        int lastDot = email.lastIndexOf('.');
        boolean looksValid = at > 0 && lastDot > at + 1 && lastDot < email.length() - 1 && !email.contains(" ");
        if (!looksValid) {
            throw new InvalidInputException("Please enter a valid email address (for example name@example.com).");
        }
        return email;
    }

    public static String requirePassword(String text) throws InvalidInputException {
        if (text == null || text.isEmpty()) {
            throw new InvalidInputException("Password cannot be empty.");
        }
        if (text.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters long.");
        }
        return text;
    }

    public static int parseAge(String text) throws InvalidInputException {
        return parseWholeNumberInRange(text, "Age", 10, 100);
    }

    public static double parseHeight(String text) throws InvalidInputException {
        return parseNumberInRange(text, "Height (cm)", 100, 250);
    }

    public static double parseWeight(String text) throws InvalidInputException {
        return parseNumberInRange(text, "Weight (kg)", 25, 300);
    }

    // Shows 2000.0 as "2000" so messages look clean
    private static String trim(double number) {
        if (number == Math.floor(number)) {
            return String.valueOf((long) number);
        }
        return String.valueOf(number);
    }
}
