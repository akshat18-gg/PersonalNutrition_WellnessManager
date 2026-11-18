package gui;

import exception.DatabaseException;
import model.DailySummary;
import model.NutritionGoal;
import service.NutritionService;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Weekly Report Screen for NutriTrack.
 * - Bar chart of calories for the last 7 days with a line for the goal.
 * - Weekly average, best day and logging streak.
 * NutritionService does the calculations; this class only shows the results.
 */
public class WeeklyReportPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM");

    private final NutritionService nutritionService = new NutritionService();

    private CalorieChartPanel chartPanel;

    private JLabel averageValue = new JLabel("-");
    private JLabel averageNote = new JLabel(" ");
    private JLabel bestDayValue = new JLabel("-");
    private JLabel bestDayNote = new JLabel(" ");
    private JLabel streakValue = new JLabel("-");
    private JLabel streakNote = new JLabel(" ");

    public WeeklyReportPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        JPanel headerPanel = UIStyles.createPageHeader("Weekly Report",
                "Calories for the last 7 days. Green: well under goal, orange: close to goal, red: over goal.");
        add(headerPanel, BorderLayout.NORTH);

        // 2. Chart
        chartPanel = new CalorieChartPanel();
        add(chartPanel, BorderLayout.CENTER);

        // 3. Three statistic cards under the chart
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatCard("Weekly Average", averageValue, averageNote));
        statsPanel.add(createStatCard("Best Day", bestDayValue, bestDayNote));
        statsPanel.add(createStatCard("Logging Streak", streakValue, streakNote));
        add(statsPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, JLabel noteLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(UIStyles.createCardBorder());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(Color.GRAY);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 19));
        valueLabel.setForeground(UIStyles.PRIMARY_GREEN);

        noteLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        noteLabel.setForeground(Color.DARK_GRAY);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(noteLabel);
        return card;
    }

    /** Recalculates the week. Called every time this screen is opened. */
    @Override
    public void refreshData() {
        try {
            int userId = Session.getUserId();
            NutritionGoal goal = nutritionService.getGoals(userId);
            Map<LocalDate, DailySummary> week = nutritionService.getLastSevenDays(userId);

            chartPanel.setData(week, goal.getTargetCalories());
            showAverage(nutritionService.getAverageCalories(week), goal.getTargetCalories());
            showBestDay(nutritionService.getBestDay(week, goal.getTargetCalories()));
            showStreak(nutritionService.getLoggingStreak(userId));

        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    private void showAverage(double average, int goalCalories) {
        if (average == 0) {
            averageValue.setText("No meals yet");
            averageNote.setText("Log a meal to see your average");
            return;
        }
        averageValue.setText(String.format("%.0f kcal", average));
        averageNote.setText("per logged day (goal " + goalCalories + " kcal)");
    }

    private void showBestDay(DailySummary bestDay) {
        if (bestDay == null) {
            bestDayValue.setText("-");
            bestDayNote.setText("No meals logged this week");
            return;
        }
        bestDayValue.setText(bestDay.getDate().format(DAY_FORMAT));
        bestDayNote.setText(String.format("%.0f kcal, closest to your goal", bestDay.getCalories()));
    }

    private void showStreak(int streak) {
        streakValue.setText(streak + (streak == 1 ? " day" : " days"));
        if (streak == 0) {
            streakNote.setText("Log a meal today to start a streak");
        } else {
            streakNote.setText("in a row with at least one meal");
        }
    }
}
