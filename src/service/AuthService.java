package service;

import dao.GoalDAO;
import dao.UserDAO;
import dao.impl.GoalDAOImpl;
import dao.impl.UserDAOImpl;
import exception.AuthenticationException;
import exception.DatabaseException;
import exception.DuplicateEntryException;
import exception.InvalidInputException;
import model.User;
import util.PasswordUtil;
import util.Session;
import util.Validator;

/**
 * Login, sign-up and logout.
 * The fields use the DAO INTERFACE types, so this class does not depend on JDBC.
 */
public class AuthService {

    private final UserDAO userDao = new UserDAOImpl();
    private final GoalDAO goalDao = new GoalDAOImpl();

    /**
     * Checks the email and password. On success the user is stored in Session
     * and returned; on failure an exception explains what went wrong.
     */
    public User login(String email, String password)
            throws InvalidInputException, AuthenticationException, DatabaseException {
        String cleanEmail = Validator.requireEmail(email);
        if (password == null || password.isEmpty()) {
            throw new InvalidInputException("Password cannot be empty.");
        }

        User user = userDao.findByEmail(cleanEmail);

        // The same message for "no such email" and "wrong password",
        // so a stranger cannot find out which emails are registered.
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new AuthenticationException("Incorrect email or password.");
        }

        Session.login(user);
        return user;
    }

    /**
     * Creates a new account. Only the HASH of the password is stored.
     * The new user also gets starting goals so the dashboard works right away.
     */
    public User register(User newUser, String plainPassword)
            throws InvalidInputException, DuplicateEntryException, DatabaseException {
        newUser.setName(Validator.requireText(newUser.getName(), "Name"));
        newUser.setEmail(Validator.requireEmail(newUser.getEmail()));
        Validator.requirePassword(plainPassword);

        newUser.setPassword(PasswordUtil.hash(plainPassword));
        int newId = userDao.insert(newUser);
        newUser.setUserId(newId);

        goalDao.save(GoalCalculator.suggestGoals(newUser, GoalCalculator.GOAL_MAINTAIN));
        return newUser;
    }

    public void logout() {
        Session.logout();
    }
}
