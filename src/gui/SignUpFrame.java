package gui;

import exception.DatabaseException;
import exception.DuplicateEntryException;
import exception.InvalidInputException;
import model.User;
import service.AuthService;
import service.GoalCalculator;
import util.Validator;

import javax.swing.*;
import java.awt.*;

/**
 * Sign-up Screen for NutriTrack.
 * Collects the profile details, validates them and creates the account through AuthService.
 * Age, gender, height, weight and activity level are needed later by the Smart Goal Calculator.
 */
public class SignUpFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JTextField ageField;
    private JComboBox<String> genderCombo;
    private JTextField heightField;
    private JTextField weightField;
    private JComboBox<String> activityCombo;

    public SignUpFrame() {
        setTitle("NutriTrack - Create Account");
        setSize(480, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 18));
        mainPanel.setBackground(UIStyles.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // Title
        JLabel titleLabel = new JLabel("Create your account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(UIStyles.PRIMARY_GREEN);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form: 9 rows of "label - input"
        nameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        confirmField = new JPasswordField();
        ageField = new JTextField();
        genderCombo = new JComboBox<>(GoalCalculator.GENDERS);
        heightField = new JTextField();
        weightField = new JTextField();
        activityCombo = new JComboBox<>(GoalCalculator.ACTIVITY_LEVELS);

        JPanel formPanel = new JPanel(new GridLayout(9, 2, 10, 12));
        formPanel.setOpaque(false);
        formPanel.add(new JLabel("Full name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Password (min 6 characters):"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("Confirm password:"));
        formPanel.add(confirmField);
        formPanel.add(new JLabel("Age (years):"));
        formPanel.add(ageField);
        formPanel.add(new JLabel("Gender:"));
        formPanel.add(genderCombo);
        formPanel.add(new JLabel("Height (cm):"));
        formPanel.add(heightField);
        formPanel.add(new JLabel("Weight (kg):"));
        formPanel.add(weightField);
        formPanel.add(new JLabel("Activity level:"));
        formPanel.add(activityCombo);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JButton createButton = new JButton("Create Account");
        UIStyles.stylePrimaryButton(createButton);
        createButton.setFont(new Font("Arial", Font.BOLD, 14));

        JButton backButton = new JButton("Back to Login");
        UIStyles.styleSecondaryButton(backButton);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createButton);
        buttonPanel.add(backButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        createButton.addActionListener(e -> handleSignUp());
        backButton.addActionListener(e -> backToLogin(""));
        getRootPane().setDefaultButton(createButton);

        add(mainPanel);
    }

    private void handleSignUp() {
        try {
            User newUser = readForm();
            String password = new String(passwordField.getPassword());

            authService.register(newUser, password);

            UIStyles.showInfo(this, "Account created. You can now log in.");
            backToLogin(newUser.getEmail());

        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DuplicateEntryException ex) {
            UIStyles.showWarning(this, ex.getMessage() + "\nTry logging in instead.");
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /**
     * Builds a User from the form. Validator throws InvalidInputException at the
     * first field that is wrong, and this method passes it on with "throws".
     */
    private User readForm() throws InvalidInputException {
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        User user = new User();
        user.setName(Validator.requireText(nameField.getText(), "Full name"));
        user.setEmail(Validator.requireEmail(emailField.getText()));
        Validator.requirePassword(password);
        if (!password.equals(confirm)) {
            throw new InvalidInputException("The two passwords do not match.");
        }
        user.setAge(Validator.parseAge(ageField.getText()));
        user.setGender((String) genderCombo.getSelectedItem());
        user.setHeight(Validator.parseHeight(heightField.getText()));
        user.setWeight(Validator.parseWeight(weightField.getText()));
        user.setActivityLevel((String) activityCombo.getSelectedItem());
        return user;
    }

    private void backToLogin(String email) {
        LoginFrame login = new LoginFrame(email);
        login.setVisible(true);
        dispose();
    }
}
