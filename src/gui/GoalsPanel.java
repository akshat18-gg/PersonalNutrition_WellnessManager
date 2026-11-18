package gui;

import dao.GoalDAO;
import dao.impl.GoalDAOImpl;
import exception.DatabaseException;
import exception.InvalidInputException;
import model.NutritionGoal;
import service.NutritionService;
import util.Session;
import util.Validator;

import javax.swing.*;
import java.awt.*;

/**
 * Goals Screen for NutriTrack.
 * Shows the user's daily targets and saves changes through GoalDAO.
 * The Profile screen can also fill this form with suggested goals.
 */
public class GoalsPanel extends JPanel implements Refreshable {

    private final GoalDAO goalDao = new GoalDAOImpl();
    private final NutritionService nutritionService = new NutritionService();

    private JTextField caloriesField;
    private JTextField proteinField;
    private JTextField carbsField;
    private JTextField fatField;
    private JTextField waterField;
    private JButton saveButton;
    private JButton reloadButton;
    private JLabel hintLabel;

    public GoalsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        reloadButton = new JButton("Undo Changes");
        UIStyles.styleSecondaryButton(reloadButton);
        reloadButton.addActionListener(e -> refreshData());

        JPanel headerPanel = UIStyles.createPageHeader("Nutrition Goals",
                "Your daily targets. For a personal suggestion, open Profile and click \"Suggest my goals\".",
                reloadButton);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Form Panel
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 15));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Target Daily Intake"),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        caloriesField = new JTextField();
        proteinField = new JTextField();
        carbsField = new JTextField();
        fatField = new JTextField();
        waterField = new JTextField();

        saveButton = new JButton("Save Goals");
        UIStyles.stylePrimaryButton(saveButton);
        saveButton.addActionListener(e -> saveGoals());

        formPanel.add(new JLabel("Target Calories (kcal):"));
        formPanel.add(caloriesField);

        formPanel.add(new JLabel("Target Protein (g):"));
        formPanel.add(proteinField);

        formPanel.add(new JLabel("Target Carbohydrates (g):"));
        formPanel.add(carbsField);

        formPanel.add(new JLabel("Target Fat (g):"));
        formPanel.add(fatField);

        formPanel.add(new JLabel("Target Water (ml):"));
        formPanel.add(waterField);

        formPanel.add(new JLabel("")); // spacer
        formPanel.add(saveButton);
        formPanel.setPreferredSize(new Dimension(480, 270));

        // Shown only after the Profile screen has filled in suggested values
        hintLabel = new JLabel(" ");
        hintLabel.setFont(new Font("Arial", Font.BOLD, 13));
        hintLabel.setForeground(UIStyles.WARNING_ORANGE);

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formWrap.setOpaque(false);
        formWrap.add(formPanel);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(hintLabel, BorderLayout.NORTH);
        centerPanel.add(formWrap, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    /** Loads the saved goals into the form. Called every time this screen is opened. */
    @Override
    public void refreshData() {
        hintLabel.setText(" ");
        try {
            fillForm(nutritionService.getGoals(Session.getUserId()));
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /**
     * Puts suggested goals into the form WITHOUT saving them.
     * The user can still adjust the numbers and must press Save Goals to keep them.
     */
    public void showSuggestedGoals(NutritionGoal suggestion, String goalType) {
        fillForm(suggestion);
        hintLabel.setText("Suggested goals to " + goalType.toLowerCase()
                + " are filled in below. Press \"Save Goals\" to keep them.");
    }

    private void fillForm(NutritionGoal goal) {
        caloriesField.setText(String.valueOf(goal.getTargetCalories()));
        proteinField.setText(UIStyles.formatNumber(goal.getTargetProtein()));
        carbsField.setText(UIStyles.formatNumber(goal.getTargetCarbs()));
        fatField.setText(UIStyles.formatNumber(goal.getTargetFat()));
        waterField.setText(String.valueOf(goal.getTargetWater()));
    }

    private void saveGoals() {
        try {
            NutritionGoal goal = new NutritionGoal();
            goal.setUserId(Session.getUserId());
            goal.setTargetCalories(Validator.parseWholeNumberInRange(caloriesField.getText(), "Target calories", 800, 10000));
            goal.setTargetProtein(Validator.parseNumberInRange(proteinField.getText(), "Target protein", 0, 500));
            goal.setTargetCarbs(Validator.parseNumberInRange(carbsField.getText(), "Target carbohydrates", 0, 1500));
            goal.setTargetFat(Validator.parseNumberInRange(fatField.getText(), "Target fat", 0, 500));
            goal.setTargetWater(Validator.parseWholeNumberInRange(waterField.getText(), "Target water", 500, 10000));

            goalDao.save(goal);

            hintLabel.setText(" ");
            UIStyles.showInfo(this, "Your goals have been saved.");

        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }
}
