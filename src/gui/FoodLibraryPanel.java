package gui;

import dao.FoodDAO;
import dao.impl.FoodDAOImpl;
import exception.DatabaseException;
import exception.DuplicateEntryException;
import exception.InvalidInputException;
import model.Food;
import util.Validator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Food Library Screen for NutriTrack.
 * Add, edit, delete and search foods. All values are per 100 g.
 * All database work goes through FoodDAO; this class has no SQL.
 */
public class FoodLibraryPanel extends JPanel implements Refreshable {

    private final FoodDAO foodDao = new FoodDAOImpl();

    private JTable foodTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel countLabel;

    // Input fields for adding food
    private JTextField nameField;
    private JTextField categoryField;
    private JTextField caloriesField;
    private JTextField proteinField;
    private JTextField carbsField;
    private JTextField fatField;
    private JButton addFoodButton;
    private JButton editButton;
    private JButton deleteButton;

    // Every food in the library, loaded once per refresh
    private List<Food> allFoods = new ArrayList<Food>();
    // Only the foods that match the search box, in the same order as the table rows
    private final List<Food> shownFoods = new ArrayList<Food>();

    public FoodLibraryPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Header Toolbar
        editButton = new JButton("Edit Selected");
        UIStyles.styleSecondaryButton(editButton);

        deleteButton = new JButton("Delete Selected");
        UIStyles.styleSecondaryButton(deleteButton);

        JPanel headerPanel = UIStyles.createPageHeader("Food Library",
                "All nutrition values are per 100 g of the food.", editButton, deleteButton);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Add-food form with the search box under it
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        topPanel.add(createFormPanel(), BorderLayout.NORTH);
        topPanel.add(createSearchBar(), BorderLayout.SOUTH);

        // 3. JTable Setup
        String[] columns = {"Food Name", "Category", "Calories (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        foodTable = new JTable(tableModel);
        UIStyles.styleTable(foodTable);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(foodTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 4. Action Listeners
        addFoodButton.addActionListener(e -> addFood());
        editButton.addActionListener(e -> editSelectedFood());
        deleteButton.addActionListener(e -> deleteSelectedFood());

        // Mouse event: double-click a row to edit that food (same as "Edit Selected").
        // MouseAdapter lets us override only mouseClicked instead of all five MouseListener methods.
        foodTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && foodTable.getSelectedRow() != -1) {
                    editSelectedFood();
                }
            }
        });
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(UIStyles.createFormBorder("Add a New Food"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(10);
        categoryField = new JTextField(10);
        caloriesField = new JTextField(6);
        proteinField = new JTextField(6);
        carbsField = new JTextField(6);
        fatField = new JTextField(6);

        addFoodButton = new JButton("Add Food");
        UIStyles.stylePrimaryButton(addFoodButton);

        // Row 1: Labels
        gbc.gridy = 0;
        gbc.gridx = 0; formPanel.add(new JLabel("Food Name:"), gbc);
        gbc.gridx = 1; formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Calories (kcal):"), gbc);
        gbc.gridx = 3; formPanel.add(new JLabel("Protein (g):"), gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Carbs (g):"), gbc);
        gbc.gridx = 5; formPanel.add(new JLabel("Fat (g):"), gbc);

        // Row 2: Inputs + Button
        gbc.gridy = 1;
        gbc.gridx = 0; formPanel.add(nameField, gbc);
        gbc.gridx = 1; formPanel.add(categoryField, gbc);
        gbc.gridx = 2; formPanel.add(caloriesField, gbc);
        gbc.gridx = 3; formPanel.add(proteinField, gbc);
        gbc.gridx = 4; formPanel.add(carbsField, gbc);
        gbc.gridx = 5; formPanel.add(fatField, gbc);
        gbc.gridx = 6; formPanel.add(addFoodButton, gbc);
        return formPanel;
    }

    private JPanel createSearchBar() {
        searchField = new JTextField(22);
        countLabel = new JLabel(" ");
        countLabel.setForeground(Color.GRAY);

        // A DocumentListener is told about every change to the text, so the
        // table is filtered while the user is still typing (no Search button needed).
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                showFilteredFoods();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                showFilteredFoods();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                showFilteredFoods();
            }
        });

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Search by name or category:"));
        searchBar.add(searchField);
        searchBar.add(countLabel);
        return searchBar;
    }

    /** Called by DashboardFrame every time this screen is opened. */
    @Override
    public void refreshData() {
        try {
            allFoods = foodDao.findAll();
            showFilteredFoods();
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /** Rebuilds the table with only the foods whose name or category contains the search text. */
    private void showFilteredFoods() {
        String searchText = searchField.getText().trim().toLowerCase();

        tableModel.setRowCount(0);
        shownFoods.clear();

        for (Food food : allFoods) {
            // The category column may be empty (NULL) for foods added outside the app
            String category = food.getCategory() == null ? "" : food.getCategory();
            boolean matches = food.getFoodName().toLowerCase().contains(searchText)
                    || category.toLowerCase().contains(searchText);
            if (matches) {
                shownFoods.add(food);
                tableModel.addRow(new Object[]{
                        food.getFoodName(),
                        category,
                        UIStyles.formatNumber(food.getCalories()),
                        UIStyles.formatNumber(food.getProtein()),
                        UIStyles.formatNumber(food.getCarbs()),
                        UIStyles.formatNumber(food.getFat())});
            }
        }
        countLabel.setText("Showing " + shownFoods.size() + " of " + allFoods.size() + " foods");
    }

    /** CREATE: validates the form and saves a new food. */
    private void addFood() {
        try {
            Food food = readFood(nameField, categoryField, caloriesField, proteinField, carbsField, fatField);
            foodDao.insert(food);

            nameField.setText("");
            categoryField.setText("");
            caloriesField.setText("");
            proteinField.setText("");
            carbsField.setText("");
            fatField.setText("");
            refreshData();

        } catch (InvalidInputException | DuplicateEntryException ex) {
            // Multi-catch: both are mistakes the user can fix, so both get the same warning dialog
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /** UPDATE: edits the selected food in a small dialog. */
    private void editSelectedFood() {
        int row = foodTable.getSelectedRow();
        if (row == -1) {
            UIStyles.showWarning(this, "Please select a food in the table first.");
            return;
        }
        Food current = shownFoods.get(row);

        JTextField nameEdit = new JTextField(current.getFoodName(), 12);
        JTextField categoryEdit = new JTextField(current.getCategory(), 12);
        JTextField caloriesEdit = new JTextField(UIStyles.formatNumber(current.getCalories()), 6);
        JTextField proteinEdit = new JTextField(UIStyles.formatNumber(current.getProtein()), 6);
        JTextField carbsEdit = new JTextField(UIStyles.formatNumber(current.getCarbs()), 6);
        JTextField fatEdit = new JTextField(UIStyles.formatNumber(current.getFat()), 6);

        JPanel editPanel = new JPanel(new GridLayout(6, 2, 6, 6));
        editPanel.add(new JLabel("Food Name:")); editPanel.add(nameEdit);
        editPanel.add(new JLabel("Category:")); editPanel.add(categoryEdit);
        editPanel.add(new JLabel("Calories (kcal):")); editPanel.add(caloriesEdit);
        editPanel.add(new JLabel("Protein (g):")); editPanel.add(proteinEdit);
        editPanel.add(new JLabel("Carbs (g):")); editPanel.add(carbsEdit);
        editPanel.add(new JLabel("Fat (g):")); editPanel.add(fatEdit);

        int result = JOptionPane.showConfirmDialog(this, editPanel, "Edit " + current.getFoodName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Food edited = readFood(nameEdit, categoryEdit, caloriesEdit, proteinEdit, carbsEdit, fatEdit);
            edited.setFoodId(current.getFoodId()); // same row in the database, new values
            foodDao.update(edited);
            refreshData();

        } catch (InvalidInputException | DuplicateEntryException ex) {
            UIStyles.showWarning(this, ex.getMessage());
        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /** DELETE: removes the selected food after warning if it is used in logged meals. */
    private void deleteSelectedFood() {
        int row = foodTable.getSelectedRow();
        if (row == -1) {
            UIStyles.showWarning(this, "Please select a food in the table first.");
            return;
        }
        Food food = shownFoods.get(row);

        try {
            String question = "Delete \"" + food.getFoodName() + "\" from the food library?";
            int usages = foodDao.countUsages(food.getFoodId());
            if (usages > 0) {
                question += "\n\nIt is used in " + usages + " logged meal item(s).\n"
                          + "Those entries will be removed from the meal history too.";
            }

            int confirm = JOptionPane.showConfirmDialog(this, question, "Confirm Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                foodDao.delete(food.getFoodId());
                refreshData();
            }

        } catch (DatabaseException ex) {
            UIStyles.showError(this, ex);
        }
    }

    /**
     * Builds a Food from six text fields. Used by both the add form and the edit dialog,
     * so both follow exactly the same rules.
     * 900 kcal per 100 g is pure fat, so nothing real can be above that.
     */
    private Food readFood(JTextField name, JTextField category, JTextField calories,
                          JTextField protein, JTextField carbs, JTextField fat) throws InvalidInputException {
        Food food = new Food();
        food.setFoodName(Validator.requireText(name.getText(), "Food name"));
        food.setCategory(Validator.requireText(category.getText(), "Category"));
        food.setCalories(Validator.parseNumberInRange(calories.getText(), "Calories", 0, 900));
        food.setProtein(Validator.parseNumberInRange(protein.getText(), "Protein", 0, 100));
        food.setCarbs(Validator.parseNumberInRange(carbs.getText(), "Carbs", 0, 100));
        food.setFat(Validator.parseNumberInRange(fat.getText(), "Fat", 0, 100));
        return food;
    }
}
