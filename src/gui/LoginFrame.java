package gui;

import exception.AuthenticationException;
import exception.DatabaseException;
import exception.InvalidInputException;
import service.AuthService;

import javax.swing.*;
import java.awt.*;

/**
 * Login Screen for NutriTrack.
 * Checks the email and password through AuthService and only then opens the dashboard.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signUpButton;

    public LoginFrame() {
        this("");
    }

    /** Opens the login screen with the email already typed in (used after sign-up). */
    public LoginFrame(String email) {
        // Frame setup
        setTitle("NutriTrack - Login");
        setSize(450, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);

        // Main panel with vertical box layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(UIStyles.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Title and Subtitle
        JLabel titleLabel = new JLabel("NutriTrack");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(UIStyles.PRIMARY_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Track better. Eat smarter. Live healthier.");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        subtitleLabel.setForeground(Color.DARK_GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Email section
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emailField = new JTextField(email, 20);
        emailField.setMaximumSize(new Dimension(300, 32));

        // Password section
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(300, 32));

        // Buttons
        loginButton = new JButton("Login");
        UIStyles.stylePrimaryButton(loginButton);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14)); // slightly larger than other primary buttons
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(300, 38));

        signUpButton = new JButton("Create a new account");
        UIStyles.styleSecondaryButton(signUpButton);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setMaximumSize(new Dimension(300, 32));

        JLabel demoLabel = new JLabel("Demo account: student@nutritrack.com / password123");
        demoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        demoLabel.setForeground(Color.GRAY);
        demoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton.addActionListener(e -> handleLogin());
        signUpButton.addActionListener(e -> openSignUp());
        // Pressing Enter anywhere in the window works like clicking Login
        getRootPane().setDefaultButton(loginButton);

        // Add components with spacing
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        mainPanel.add(emailLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(emailField);
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(passwordLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(25));

        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(signUpButton);
        mainPanel.add(Box.createVerticalStrut(18));
        mainPanel.add(demoLabel);

        add(mainPanel);
    }

    /**
     * Each kind of problem gets its own catch block, most specific first:
     * a typing mistake and a wrong password are warnings the user can fix,
     * a database failure is an error.
     */
    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        try {
            authService.login(email, password);

            // Login worked: open the Dashboard and close this window
            DashboardFrame dashboard = new DashboardFrame();
            dashboard.setVisible(true);
            dispose();

        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (AuthenticationException ex) {
            UIStyles.showWarning(this, ex.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex.getMessage()
                    + "\nPlease check that MySQL is running and config/db.properties is correct.", ex);
        }
    }

    private void openSignUp() {
        SignUpFrame signUp = new SignUpFrame();
        signUp.setVisible(true);
        dispose();
    }
}
