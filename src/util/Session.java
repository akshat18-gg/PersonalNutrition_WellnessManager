package util;

import model.User;

/**
 * Remembers who is logged in while the app is running.
 *
 * Every screen asks Session for the current user id instead of using a
 * hardcoded "user_id = 1", so each user only sees their own data.
 */
public final class Session {

    private static User currentUser;

    private Session() {
        // only static methods, no objects needed
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getUserId() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return currentUser.getUserId();
    }
}
