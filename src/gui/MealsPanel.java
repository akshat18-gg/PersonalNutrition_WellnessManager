package gui;

import dao.FoodDAO;
import dao.MealDAO;
import dao.impl.FoodDAOImpl;
import dao.impl.MealDAOImpl;
import exception.DatabaseException;
import exception.InvalidInputException;
import model.Food;
import model.Meal;
import model.MealItem;
import service.NutritionService;
import util.Session;
import util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Meals Screen for NutriTrack.
 * - Pick a date (today by default) and see everything eaten on that day.
 * - Add a food to Breakfast / Lunch / Snack / Dinner of that day.
 * - Delete a single item.
 * All database work goes through MealDAO and FoodDAO; this class has no SQL.
 */
public class MealsPanel extends JPanel implements Refreshable {

    // The fields have the INTERFACE type; only "new ...Impl()" names the JDBC class
    private final MealDAO mealDao = new MealDAOImpl();
    private final FoodDAO foodDao = new FoodDAOImpl();
    private final NutritionService nutritionService = new NutritionService();

    private JSpinner dateSpinner;
    private JComboBox<String> mealTypeCombo;
    private JComboBox<Food> foodCombo;
    private JTextField quantityField;
    private JButton addMealButton;
    private JButton deleteItemButton;
    private JTable mealsTable;
    private DefaultTableModel tableModel;
    private JLabel totalsLabel;

    // The meal item shown on each table row (row 0 is rowItems.get(0), and so on).
    // The table only shows text, so this list tells us WHICH item to delete.
    private final List<MealItem> rowItems = new ArrayList<MealItem>();

    public MealsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header
        deleteItemButton = new JButton("Delete Selected Item");
        UIStyles.styleSecondaryButton(deleteItemButton);

        JPanel headerPanel = UIStyles.createPageHeader("Daily Meals",
                "Log what you eat and look back at any day.", deleteItemButton);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Date picker + input form, stacked above the table
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(createDateBar(), BorderLayout.NORTH);
        topPanel.add(createFormPanel(), BorderLayout.CENTER);

        // 3. Table of the day's meal items
        String[] columns = {"Meal", "Food", "Quantity (g)", "Calories (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        mealsTable = new JTable(tableModel);
        UIStyles.styleTable(mealsTable);

        // 4. Totals per meal type under the table
        totalsLabel = new JLabel(" ");
        totalsLabel.setFont(new Font("Arial", Font.BOLD, 13));
        totalsLabel.setForeground(UIStyles.PRIMARY_GREEN);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(mealsTable), BorderLayout.CENTER);
        centerPanel.add(totalsLabel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // 5. Action Listeners
        addMealButton.addActionListener(e -> addMealItem());
        deleteItemButton.addActionListener(e -> deleteSelectedItem());
    }

    private JPanel createDateBar() {
        // A JSpinner with a date model is the date picker that comes with the JDK.
        // The last argument (today) is the latest date it allows.
        Date today = toDate(LocalDate.now());
        dateSpinner = new JSpinner(new SpinnerDateModel(today, null, new Date(), Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy"));
        dateSpinner.setPreferredSize(new Dimension(130, 26));

        JButton previousButton = new JButton("< Previous Day");
        JButton nextButton = new JButton("Next Day >");
        JButton todayButton = new JButton("Today");
        UIStyles.styleSecondaryButton(previousButton);
        UIStyles.styleSecondaryButton(nextButton);
        UIStyles.styleSecondaryButton(todayButton);

        previousButton.addActionListener(e -> setSelectedDate(getSelectedDate().minusDays(1)));
        nextButton.addActionListener(e -> setSelectedDate(getSelectedDate().plusDays(1)));
        todayButton.addActionListener(e -> setSelectedDate(LocalDate.now()));
        // Fires whenever the date changes, by typing, by the arrows or by the buttons above
        dateSpinner.addChangeListener(e -> loadMeals());

        JPanel dateBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dateBar.setOpaque(false);
        dateBar.add(new JLabel("Showing meals for:"));
        dateBar.add(dateSpinner);
        dateBar.add(previousButton);
        dateBar.add(nextButton);
        dateBar.add(todayButton);
        return dateBar;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(UIStyles.createFormBorder("Add Food to a Meal"));

        mealTypeCombo = new JComboBox<>(NutritionService.MEAL_TYPES);

        foodCombo = new JComboBox<>();
        foodCombo.setPreferredSize(new Dimension(170, 26));

        quantityField = new JTextField("100", 6); // grams
        addMealButton = new JButton("Add to Meal");
        UIStyles.stylePrimaryButton(addMealButton);

        formPanel.add(new JLabel("Meal Type:"));
        formPanel.add(mealTypeCombo);
        formPanel.add(new JLabel("Select Food:"));
        formPanel.add(foodCombo);
        formPanel.add(new JLabel("Quantity (g):"));
        formPanel.add(quantityField);
        formPanel.add(addMealButton);
        return formPanel;
    }

    /** Called by DashboardFrame every time this screen is opened. */
    @Override
    public void refreshData() {
        loadFoodList();
        loadMeals();
    }

    /** Fills the food drop-down from the Food Library (it may have changed since last time). */
    private void loadFoodList() {
        foodCombo.removeAllItems();
        try {
            for (Food food : foodDao.findAll()) {
                foodCombo.addItem(food); // the combo shows food.toString(), which is the name
            }
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /** Shows the meal items of the selected date and the calories per meal type. */
    private void loadMeals() {
        tableModel.setRowCount(0);
        rowItems.clear();

        try {
            List<Meal> meals = mealDao.findByDate(Session.getUserId(), getSelectedDate());

            for (Meal meal : meals) {
                for (MealItem item : meal.getItems()) {
                    tableModel.addRow(new Object[]{
                            meal.getMealType(),
                            item.getFood().getFoodName(),
                            UIStyles.formatNumber(item.getQuantity()),
                            String.format("%.0f", item.getCalories()),
                            String.format("%.1f", item.getProtein()),
                            String.format("%.1f", item.getCarbs()),
                            String.format("%.1f", item.getFat())});
                    rowItems.add(item);
                }
            }
            showTotals(nutritionService.getCaloriesByMealType(meals));

        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    // Example: "Breakfast: 512 kcal   |   Lunch: 658 kcal   |   ...   |   Total: 1170 kcal"
    private void showTotals(Map<String, Double> caloriesByMealType) {
        StringBuilder text = new StringBuilder();
        double dayTotal = 0;
        for (Map.Entry<String, Double> entry : caloriesByMealType.entrySet()) {
            text.append(String.format("%s: %.0f kcal   |   ", entry.getKey(), entry.getValue()));
            dayTotal += entry.getValue();
        }
        text.append(String.format("Total: %.0f kcal", dayTotal));
        totalsLabel.setText(text.toString());
    }

    /**
     * Adds the chosen food to the chosen meal of the selected date.
     * MealDAO reuses that day's meal if it already exists.
     */
    private void addMealItem() {
        String mealType = (String) mealTypeCombo.getSelectedItem();
        Food food = (Food) foodCombo.getSelectedItem();

        try {
            if (food == null) {
                throw new InvalidInputException("Please select a food. Add foods in the Food Library first.");
            }
            double quantity = Validator.parseQuantity(quantityField.getText());

            mealDao.addItem(Session.getUserId(), mealType, getSelectedDate(), food.getFoodId(), quantity);

            quantityField.setText("100");
            loadMeals();

        } catch (InvalidInputException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    private void deleteSelectedItem() {
        int row = mealsTable.getSelectedRow();
        if (row == -1) {
            UIStyles.showWarning(this, "Please select a row in the table first.");
            return;
        }

        MealItem item = rowItems.get(row);
        String mealType = (String) tableModel.getValueAt(row, 0);
        String question = "Remove " + item.getFood().getFoodName() + " (" + UIStyles.formatNumber(item.getQuantity())
                + " g) from " + mealType + "?";

        int confirm = JOptionPane.showConfirmDialog(this, question, "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            mealDao.deleteItem(item.getMealItemId());
            loadMeals();
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    // ---------- date helpers ----------
    // JSpinner works with the old java.util.Date, the rest of the app with LocalDate,
    // so these two methods convert between them.

    private LocalDate getSelectedDate() {
        Date date = (Date) dateSpinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void setSelectedDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            return; // meals cannot be logged for the future
        }
        dateSpinner.setValue(toDate(date)); // this triggers the change listener, which reloads the table
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
