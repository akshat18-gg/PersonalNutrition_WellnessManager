package gui;

import model.NutritionGoal;
import service.AuthService;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main window of NutriTrack: sidebar on the left, the current screen on the right.
 *
 * A CardLayout holds all screens and shows one at a time. Every screen
 * implements Refreshable, so showScreen() can reload ANY of them with the same
 * single call, screen.refreshData(), without knowing which panel it is.
 */
public class DashboardFrame extends JFrame {

    // Names of the screens inside the CardLayout
    private static final String DASHBOARD = "DASHBOARD";
    private static final String MEALS = "MEALS";
    private static final String FOOD_LIBRARY = "FOOD_LIBRARY";
    private static final String WATER = "WATER";
    private static final String GOALS = "GOALS";
    private static final String PROFILE = "PROFILE";
    private static final String WEEKLY_REPORT = "WEEKLY_REPORT";

    private final AuthService authService = new AuthService();

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebar;
    // Kept as a field because the Profile screen sends suggested goals to it
    private GoalsPanel goalsPanel;
    // Background thread that reminds the user to drink water (multithreading)
    private WaterReminder waterReminder;

    // screen name -> the screen, seen only as "something that can refresh itself"
    private final Map<String, Refreshable> screens = new LinkedHashMap<String, Refreshable>();
    // screen name -> its sidebar button, so the open screen can be highlighted
    private final Map<String, JButton> navButtons = new LinkedHashMap<String, JButton>();

    public DashboardFrame() {
        setTitle("NutriTrack - Personal Nutrition & Wellness Manager");
        setSize(1100, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // 1. Sidebar Navigation: one row per screen, a spacer, and Logout
        sidebar = new JPanel();
        sidebar.setLayout(new GridLayout(9, 1, 5, 10));
        sidebar.setBackground(UIStyles.SIDEBAR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidebar.setPreferredSize(new Dimension(190, 0));

        // 2. Content Area with CardLayout
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        // 3. Screens (each call adds the card AND its sidebar button)
        addScreen(DASHBOARD, "Dashboard", new DashboardHomePanel());
        addScreen(MEALS, "Meals", new MealsPanel());
        addScreen(FOOD_LIBRARY, "Food Library", new FoodLibraryPanel());
        addScreen(WATER, "Water", new WaterPanel());
        goalsPanel = new GoalsPanel();
        addScreen(GOALS, "Goals", goalsPanel);
        addScreen(PROFILE, "Profile", new ProfilePanel(this));
        addScreen(WEEKLY_REPORT, "Weekly Report", new WeeklyReportPanel());

        sidebar.add(new JLabel("")); // spacer

        JButton logoutButton = new JButton("Logout");
        UIStyles.styleNavButton(logoutButton, false);
        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        add(sidebar, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        // Start on the dashboard with fresh data
        showScreen(DASHBOARD);

        // Start the background water reminder for the logged-in user
        waterReminder = new WaterReminder(this, Session.getUserId());
        waterReminder.start();

        // Window event: when the user closes the window, stop the reminder thread first.
        // WindowAdapter lets us override only the one method we need (windowClosing).
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                waterReminder.stop();
            }
        });
    }

    /**
     * Registers one screen: puts the panel into the CardLayout, remembers it as a
     * Refreshable, and creates the sidebar button that opens it.
     */
    private void addScreen(final String name, String buttonText, JPanel panel) {
        mainContentPanel.add(panel, name);

        // Every screen panel implements Refreshable. A parameter can have only one
        // type, so the panel arrives as a JPanel and is cast to its second type here.
        screens.put(name, (Refreshable) panel);

        JButton button = new JButton(buttonText);
        button.addActionListener(e -> showScreen(name));
        navButtons.put(name, button);
        sidebar.add(button);
    }

    /** Refreshes a screen from the database and then shows it. */
    private void showScreen(String name) {
        Refreshable screen = screens.get(name);
        screen.refreshData(); // the same call works for every screen

        cardLayout.show(mainContentPanel, name);

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean isOpen = entry.getKey().equals(name);
            UIStyles.styleNavButton(entry.getValue(), isOpen);
        }
    }

    /**
     * Called by the Profile screen: opens the Goals screen and fills its form
     * with the suggested goals. showScreen() first loads the saved goals, so
     * the suggestion must be put in AFTER it.
     */
    public void showSuggestedGoals(NutritionGoal suggestion, String goalType) {
        showScreen(GOALS);
        goalsPanel.showSuggestedGoals(suggestion, goalType);
    }

    private void logout() {
        waterReminder.stop(); // the next user must not get this user's reminders
        authService.logout();
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
        dispose();
    }
}
