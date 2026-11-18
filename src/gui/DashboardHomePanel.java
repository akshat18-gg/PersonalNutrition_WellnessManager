package gui;

import dao.MealDAO;
import dao.impl.MealDAOImpl;
import exception.DatabaseException;
import exception.NutriTrackException;
import model.DailySummary;
import model.FoodSuggestion;
import model.Meal;
import model.NutritionGoal;
import service.CsvReportExporter;
import service.NutritionService;
import service.ReportExporter;
import service.TextReportExporter;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Dashboard home screen.
 * - Greets the logged-in user.
 * - Five progress cards for TODAY: calories, protein, carbs, fat, water.
 * - "What should I eat next?" card with three food suggestions.
 * - Export button that saves today's report as TXT or CSV.
 */
public class DashboardHomePanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
    private static final String REPORTS_FOLDER = "reports";

    private final NutritionService nutritionService = new NutritionService();
    private final MealDAO mealDao = new MealDAOImpl();

    private JLabel welcomeLabel;
    private JLabel dateLabel;

    private MetricCard caloriesCard;
    private MetricCard proteinCard;
    private MetricCard carbsCard;
    private MetricCard fatCard;
    private MetricCard waterCard;

    private JLabel gapLabel;
    private JLabel[] suggestionLabels = new JLabel[3];

    public DashboardHomePanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        add(createHeader(), BorderLayout.NORTH);

        // Same 2 x 3 grid as before; the sixth cell now holds the suggestions
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        cardsPanel.setOpaque(false);

        caloriesCard = new MetricCard("Calories", "kcal", true);
        proteinCard = new MetricCard("Protein", "g", true);
        carbsCard = new MetricCard("Carbohydrates", "g", true);
        fatCard = new MetricCard("Fat", "g", true);
        waterCard = new MetricCard("Water", "ml", false); // water is a target to reach, not a limit

        cardsPanel.add(caloriesCard);
        cardsPanel.add(proteinCard);
        cardsPanel.add(carbsCard);
        cardsPanel.add(fatCard);
        cardsPanel.add(waterCard);
        cardsPanel.add(createSuggestionCard());

        add(cardsPanel, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(UIStyles.PRIMARY_GREEN);

        dateLabel = new JLabel(" ");
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        dateLabel.setForeground(Color.DARK_GRAY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        textPanel.setOpaque(false);
        textPanel.add(welcomeLabel);
        textPanel.add(dateLabel);

        JButton exportButton = new JButton("Export Today's Report");
        UIStyles.stylePrimaryButton(exportButton);
        exportButton.addActionListener(e -> exportReport());

        // FlowLayout keeps the button at its normal size instead of stretching it
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.add(exportButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(textPanel, BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createSuggestionCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(UIStyles.createCardBorder());

        JLabel titleLabel = new JLabel("What should I eat next?");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(Color.GRAY);

        gapLabel = new JLabel(" ");
        gapLabel.setFont(new Font("Arial", Font.BOLD, 13));
        gapLabel.setForeground(UIStyles.PRIMARY_GREEN);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(gapLabel);

        for (int i = 0; i < suggestionLabels.length; i++) {
            suggestionLabels[i] = new JLabel(" ");
            suggestionLabels[i].setFont(new Font("Arial", Font.PLAIN, 13));
            card.add(Box.createVerticalStrut(8));
            card.add(suggestionLabels[i]);
        }
        return card;
    }

    /**
     * Reloads today's numbers. Called by DashboardFrame every time this screen is opened.
     *
     * MULTITHREADING: the database work runs on a background thread with a SwingWorker,
     * so the window stays responsive (buttons, sidebar) while the queries run.
     *  - doInBackground() runs on a worker thread: it only reads data, it never touches Swing.
     *  - done() runs back on Swing's Event Dispatch Thread: it is safe to update the labels there.
     */
    @Override
    public void refreshData() {
        welcomeLabel.setText("Welcome, " + Session.getCurrentUser().getName() + "!");
        dateLabel.setText("Here is your progress for today, " + LocalDate.now().format(DATE_FORMAT) + ".");
        gapLabel.setText("Loading...");

        // Read the id here, on the EDT, and give it to the worker thread as a final value
        final int userId = Session.getUserId();

        SwingWorker<TodayData, Void> worker = new SwingWorker<TodayData, Void>() {
            @Override
            protected TodayData doInBackground() throws Exception {
                return loadTodayData(userId); // background thread: database only
            }

            @Override
            protected void done() {
                try {
                    showTodayData(get()); // get() returns what doInBackground() returned
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); // keep the "interrupted" signal
                } catch (ExecutionException ex) {
                    // Any exception thrown in doInBackground() arrives here, wrapped.
                    // getCause() gives back the original one.
                    Throwable cause = ex.getCause();
                    if (cause instanceof NutriTrackException) {
                        UIStyles.showError(DashboardHomePanel.this, (NutriTrackException) cause);
                    } else {
                        UIStyles.showError(DashboardHomePanel.this, "Could not load today's data.", ex);
                    }
                }
            }
        };
        worker.execute(); // starts the background thread and returns at once
    }

    /** Runs on the background thread. Collects everything the screen needs in one object. */
    private TodayData loadTodayData(int userId) throws DatabaseException {
        TodayData data = new TodayData();
        data.summary = nutritionService.getDailySummary(userId, LocalDate.now());
        data.goal = nutritionService.getGoals(userId);
        data.gapNutrient = nutritionService.findBiggestGap(data.summary, data.goal);
        if (data.gapNutrient != null) {
            data.suggestions = nutritionService.suggestFoods(data.gapNutrient, data.summary, data.goal);
        }
        return data;
    }

    /** Runs on the EDT. Puts the loaded numbers on the screen. */
    private void showTodayData(TodayData data) {
        DailySummary today = data.summary;
        NutritionGoal goal = data.goal;

        caloriesCard.update(today.getCalories(), goal.getTargetCalories());
        proteinCard.update(today.getProtein(), goal.getTargetProtein());
        carbsCard.update(today.getCarbs(), goal.getTargetCarbs());
        fatCard.update(today.getFat(), goal.getTargetFat());
        waterCard.update(today.getWaterMl(), goal.getTargetWater());

        showSuggestions(data.gapNutrient, data.suggestions);
    }

    private void showSuggestions(String nutrient, List<FoodSuggestion> suggestions) {
        for (JLabel label : suggestionLabels) {
            label.setText(" ");
        }

        if (nutrient == null) {
            gapLabel.setText("All of today's food goals are met. Well done!");
            return;
        }

        String unit = NutritionService.getUnit(nutrient);
        gapLabel.setText("Your biggest gap today: " + nutrient.toLowerCase());

        for (int i = 0; i < suggestions.size() && i < suggestionLabels.length; i++) {
            FoodSuggestion suggestion = suggestions.get(i);
            // A JLabel understands simple HTML, which lets one label show two lines
            suggestionLabels[i].setText(String.format(
                    "<html><b>%d. %s</b> - %.0f g<br><font color='gray'>gives about %.0f %s %s</font></html>",
                    i + 1, suggestion.getFood().getFoodName(), suggestion.getGrams(),
                    suggestion.getNutrientAmount(), unit, describe(nutrient)));
        }
    }

    // "gives about 50 g protein" reads well, "gives about 500 kcal calories" does not
    private String describe(String nutrient) {
        if (NutritionService.CALORIES.equals(nutrient)) {
            return "";
        }
        return nutrient.toLowerCase();
    }

    /**
     * Saves today's report in the format the user picks.
     * The variable 'exporter' has the INTERFACE type, so the export call below
     * is the same line of code for TXT and for CSV.
     */
    private void exportReport() {
        ReportExporter[] exporters = {new TextReportExporter(), new CsvReportExporter()};
        String[] formatNames = new String[exporters.length];
        for (int i = 0; i < exporters.length; i++) {
            formatNames[i] = exporters[i].getFormatName();
        }

        int choice = JOptionPane.showOptionDialog(this, "Choose a format for today's report:", "Export Report",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, formatNames, formatNames[0]);
        if (choice < 0) {
            return; // the dialog was closed without choosing
        }
        ReportExporter exporter = exporters[choice];

        try {
            int userId = Session.getUserId();
            LocalDate today = LocalDate.now();
            DailySummary summary = nutritionService.getDailySummary(userId, today);
            NutritionGoal goal = nutritionService.getGoals(userId);
            List<Meal> meals = mealDao.findByDate(userId, today);

            File file = exporter.export(Session.getCurrentUser(), summary, goal, meals, new File(REPORTS_FOLDER));
            UIStyles.showInfo(this, "Report saved to:\n" + file.getAbsolutePath());

        } catch (DatabaseException | IOException ex) {
            // Multi-catch: reading from the database and writing the file can fail in
            // different ways, but the user gets the same kind of message for both.
            UIStyles.showError(this, "The report could not be exported.\n" + ex.getMessage(), ex);
        }
    }

    /**
     * Small holder for everything the background thread loads, so done() can
     * receive it as ONE object. 'static' because it does not need the panel itself.
     */
    private static class TodayData {
        DailySummary summary;
        NutritionGoal goal;
        String gapNutrient;
        List<FoodSuggestion> suggestions = new ArrayList<FoodSuggestion>();
    }
}
