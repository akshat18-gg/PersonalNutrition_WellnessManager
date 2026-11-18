package gui;

import dao.WaterDAO;
import dao.impl.WaterDAOImpl;
import exception.DatabaseException;
import exception.InvalidInputException;
import model.NutritionGoal;
import model.WaterIntake;
import service.NutritionService;
import util.Session;
import util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Water Screen for NutriTrack.
 * - Shows today's water entries and the total against the daily goal.
 * - Add any amount, or use the quick +250 / +500 / +750 ml buttons.
 * All database work goes through WaterDAO; this class has no SQL.
 */
public class WaterPanel extends JPanel implements Refreshable {

    private static final int[] QUICK_AMOUNTS_ML = {250, 500, 750};
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    private final WaterDAO waterDao = new WaterDAOImpl();
    private final NutritionService nutritionService = new NutritionService();

    private JTextField amountField;
    private JButton addWaterButton;
    private JButton deleteButton;
    private JLabel totalLabel;
    private JProgressBar progressBar;
    private JTable waterTable;
    private DefaultTableModel tableModel;

    // The entry shown on each table row, so we know which one to delete
    private final List<WaterIntake> rowEntries = new ArrayList<WaterIntake>();

    public WaterPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        deleteButton = new JButton("Delete Selected");
        UIStyles.styleSecondaryButton(deleteButton);

        JPanel headerPanel = UIStyles.createPageHeader("Water Intake",
                "Track the water you drink today.", deleteButton);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Input form with quick-add buttons, and today's progress under it
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(createFormPanel(), BorderLayout.NORTH);
        topPanel.add(createProgressPanel(), BorderLayout.SOUTH);

        // 3. Table of Water Entries
        String[] columns = {"Time", "Amount (ml)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        waterTable = new JTable(tableModel);
        UIStyles.styleTable(waterTable);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(waterTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 4. Action Listeners
        addWaterButton.addActionListener(e -> addTypedAmount());
        deleteButton.addActionListener(e -> deleteSelectedEntry());
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Log Water"));

        amountField = new JTextField("250", 8);
        addWaterButton = new JButton("Add Water");
        UIStyles.stylePrimaryButton(addWaterButton);

        formPanel.add(new JLabel("Amount (ml):"));
        formPanel.add(amountField);
        formPanel.add(addWaterButton);
        formPanel.add(Box.createHorizontalStrut(20));
        formPanel.add(new JLabel("Quick add:"));

        // One button per common glass / bottle size
        for (int amount : QUICK_AMOUNTS_ML) {
            final int quickAmount = amount; // a lambda may only use variables that never change
            JButton quickButton = new JButton("+" + quickAmount + " ml");
            UIStyles.styleSecondaryButton(quickButton);
            quickButton.addActionListener(e -> saveWater(quickAmount));
            formPanel.add(quickButton);
        }
        return formPanel;
    }

    private JPanel createProgressPanel() {
        totalLabel = new JLabel("Today: -");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setForeground(UIStyles.PRIMARY_GREEN);

        progressBar = new JProgressBar(0, 100);
        UIStyles.styleProgressBar(progressBar);
        progressBar.setPreferredSize(new Dimension(100, 20));

        JPanel progressPanel = new JPanel(new BorderLayout(15, 0));
        progressPanel.setOpaque(false);
        progressPanel.add(totalLabel, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        return progressPanel;
    }

    /** Reloads today's entries and total. Called every time this screen is opened. */
    @Override
    public void refreshData() {
        tableModel.setRowCount(0);
        rowEntries.clear();

        try {
            int userId = Session.getUserId();
            LocalDate today = LocalDate.now();

            int totalMl = 0;
            for (WaterIntake entry : waterDao.findByDate(userId, today)) {
                tableModel.addRow(new Object[]{entry.getRecordedAt().format(TIME_FORMAT), entry.getAmountMl()});
                rowEntries.add(entry);
                totalMl += entry.getAmountMl();
            }

            NutritionGoal goal = nutritionService.getGoals(userId);
            showProgress(totalMl, goal.getTargetWater());

        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    private void showProgress(int totalMl, int goalMl) {
        int percent = 0;
        if (goalMl > 0) {
            percent = (int) Math.round(totalMl * 100.0 / goalMl);
        }
        totalLabel.setText("Today: " + totalMl + " / " + goalMl + " ml");
        progressBar.setValue(Math.min(100, percent));
        progressBar.setString(percent >= 100 ? "Goal reached (" + percent + "%)" : percent + "%");
    }

    /** "Add Water" button: validates the typed amount first. */
    private void addTypedAmount() {
        try {
            int amount = Validator.parseWaterAmount(amountField.getText());
            saveWater(amount);
            amountField.setText("250");
        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        }
    }

    /** Saves one entry with the current time. Used by Add Water and by the quick buttons. */
    private void saveWater(int amountMl) {
        try {
            WaterIntake entry = new WaterIntake(0, Session.getUserId(), amountMl, LocalDateTime.now());
            waterDao.insert(entry);
            refreshData();
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    private void deleteSelectedEntry() {
        int row = waterTable.getSelectedRow();
        if (row == -1) {
            UIStyles.showWarning(this, "Please select a row in the table first.");
            return;
        }
        WaterIntake entry = rowEntries.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete the " + entry.getAmountMl() + " ml entry from " + entry.getRecordedAt().format(TIME_FORMAT) + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            waterDao.delete(entry.getWaterId());
            refreshData();
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }
}
