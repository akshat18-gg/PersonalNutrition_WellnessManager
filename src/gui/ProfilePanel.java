package gui;

import dao.UserDAO;
import dao.impl.UserDAOImpl;
import exception.DatabaseException;
import exception.InvalidInputException;
import model.NutritionGoal;
import model.User;
import service.GoalCalculator;
import util.Session;
import util.Validator;

import javax.swing.*;
import java.awt.*;

/**
 * Profile Screen for NutriTrack.
 * - Edit the personal details given at sign-up.
 * - See BMI (with its category), BMR and daily calorie need.
 * - Smart Goal Calculator: one click sends suggested goals to the Goals screen.
 * GoalCalculator holds all the formulas; this class only shows the results.
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private final UserDAO userDao = new UserDAOImpl();
    // Needed to open the Goals screen after a suggestion has been calculated
    private final DashboardFrame dashboard;

    private JTextField nameField;
    private JTextField ageField;
    private JComboBox<String> genderCombo;
    private JTextField heightField;
    private JTextField weightField;
    private JComboBox<String> activityCombo;

    private JLabel emailLabel;
    private JLabel bmiLabel;
    private JLabel bmrLabel;
    private JLabel dailyCaloriesLabel;
    private JComboBox<String> goalTypeCombo;

    public ProfilePanel(DashboardFrame dashboard) {
        this.dashboard = dashboard;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        JPanel headerPanel = UIStyles.createPageHeader("My Profile",
                "Your details are used to calculate BMI and to suggest daily goals.");
        add(headerPanel, BorderLayout.NORTH);

        // 2. Details form on the left, body stats and goal calculator on the right
        JPanel rightColumn = new JPanel(new GridLayout(2, 1, 0, 15));
        rightColumn.setOpaque(false);
        rightColumn.add(createStatsPanel());
        rightColumn.add(createCalculatorPanel());

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(createDetailsPanel());
        columns.add(rightColumn);
        add(columns, BorderLayout.CENTER);
    }

    private JPanel createDetailsPanel() {
        nameField = new JTextField();
        ageField = new JTextField();
        genderCombo = new JComboBox<>(GoalCalculator.GENDERS);
        heightField = new JTextField();
        weightField = new JTextField();
        activityCombo = new JComboBox<>(GoalCalculator.ACTIVITY_LEVELS);
        emailLabel = new JLabel(" ");

        JButton saveButton = new JButton("Save Profile");
        UIStyles.stylePrimaryButton(saveButton);
        saveButton.addActionListener(e -> saveProfile());

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 14));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("My Details"),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailLabel);
        formPanel.add(new JLabel("Full name:"));
        formPanel.add(nameField);
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
        formPanel.add(new JLabel("")); // spacer
        formPanel.add(saveButton);
        return formPanel;
    }

    private JPanel createStatsPanel() {
        bmiLabel = new JLabel("-");
        bmiLabel.setFont(new Font("Arial", Font.BOLD, 19));

        bmrLabel = new JLabel(" ");
        dailyCaloriesLabel = new JLabel(" ");

        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 0, 6));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Body Stats"),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel bmiTitle = new JLabel("Body Mass Index (BMI)");
        bmiTitle.setForeground(Color.GRAY);

        statsPanel.add(bmiTitle);
        statsPanel.add(bmiLabel);
        statsPanel.add(bmrLabel);
        statsPanel.add(dailyCaloriesLabel);
        return statsPanel;
    }

    private JPanel createCalculatorPanel() {
        goalTypeCombo = new JComboBox<>(GoalCalculator.GOAL_TYPES);
        goalTypeCombo.setSelectedItem(GoalCalculator.GOAL_MAINTAIN);

        JButton suggestButton = new JButton("Suggest my goals");
        UIStyles.stylePrimaryButton(suggestButton);
        suggestButton.addActionListener(e -> suggestGoals());

        JPanel choiceRow = new JPanel(new BorderLayout(10, 0));
        choiceRow.setOpaque(false);
        choiceRow.add(new JLabel("I want to:"), BorderLayout.WEST);
        choiceRow.add(goalTypeCombo, BorderLayout.CENTER);

        JLabel explainLabel = new JLabel("<html>Calculates calories, protein, carbs, fat and water from your "
                + "saved details, then opens the Goals screen with the numbers filled in.</html>");
        explainLabel.setForeground(Color.GRAY);

        // Three rows stacked top to bottom: choice, explanation, button.
        // (A single FlowLayout row was too wide here and pushed the button out of view.)
        JPanel calculatorPanel = new JPanel(new BorderLayout(0, 10));
        calculatorPanel.setBackground(Color.WHITE);
        calculatorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Smart Goal Calculator"),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        calculatorPanel.add(choiceRow, BorderLayout.NORTH);
        calculatorPanel.add(explainLabel, BorderLayout.CENTER);
        calculatorPanel.add(suggestButton, BorderLayout.SOUTH);
        return calculatorPanel;
    }

    /** Shows the logged-in user's saved details. Called every time this screen is opened. */
    @Override
    public void refreshData() {
        User user = Session.getCurrentUser();

        emailLabel.setText(user.getEmail());
        nameField.setText(user.getName());
        ageField.setText(String.valueOf(user.getAge()));
        genderCombo.setSelectedItem(user.getGender());
        heightField.setText(UIStyles.formatNumber(user.getHeight()));
        weightField.setText(UIStyles.formatNumber(user.getWeight()));
        activityCombo.setSelectedItem(user.getActivityLevel());

        showBodyStats(user);
    }

    private void showBodyStats(User user) {
        double bmi = GoalCalculator.calculateBmi(user.getWeight(), user.getHeight());
        String category = GoalCalculator.getBmiCategory(bmi);

        bmiLabel.setText(String.format("%.1f  (%s)", bmi, category));
        bmiLabel.setForeground(getBmiColor(category));

        bmrLabel.setText(String.format("BMR (calories burned at rest): %.0f kcal",
                GoalCalculator.calculateBmr(user)));
        dailyCaloriesLabel.setText(String.format("Daily calories to stay at this weight: %.0f kcal",
                GoalCalculator.calculateDailyCalories(user)));
    }

    private Color getBmiColor(String category) {
        if ("Normal".equals(category)) {
            return UIStyles.PRIMARY_GREEN;
        } else if ("Obese".equals(category)) {
            return UIStyles.DANGER_RED;
        }
        return UIStyles.WARNING_ORANGE; // Underweight or Overweight
    }

    private void saveProfile() {
        User current = Session.getCurrentUser();

        try {
            // Work on a copy: if validation or the database fails, the logged-in user is untouched
            User updated = new User(current.getUserId(), current.getName(), current.getEmail(),
                    current.getPassword(), current.getAge(), current.getGender(),
                    current.getHeight(), current.getWeight(), current.getActivityLevel());

            updated.setName(Validator.requireText(nameField.getText(), "Full name"));
            updated.setAge(Validator.parseAge(ageField.getText()));
            updated.setGender((String) genderCombo.getSelectedItem());
            updated.setHeight(Validator.parseHeight(heightField.getText()));
            updated.setWeight(Validator.parseWeight(weightField.getText()));
            updated.setActivityLevel((String) activityCombo.getSelectedItem());

            userDao.updateProfile(updated);

            Session.login(updated); // the rest of the app now sees the new details
            refreshData();
            UIStyles.showInfo(this, "Your profile has been saved.");

        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /** Calculates goals from the saved profile and hands them to the Goals screen. */
    private void suggestGoals() {
        String goalType = (String) goalTypeCombo.getSelectedItem();
        NutritionGoal suggestion = GoalCalculator.suggestGoals(Session.getCurrentUser(), goalType);
        dashboard.showSuggestedGoals(suggestion, goalType);
    }
}
