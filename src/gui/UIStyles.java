package gui;

import exception.NutriTrackException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

/**
 * Shared look-and-feel helpers for NutriTrack screens.
 * - colours used on every screen
 * - stylePrimaryButton: green button that renders correctly on macOS (Aqua) and Windows.
 * - createPageHeader:   page title + grey subtitle on the left, action buttons on the right.
 * - styleProgressBar / getProgressColor: the green / orange / red progress bars.
 * - showInfo / showWarning / showError: the dialogs every screen uses for messages.
 */
public final class UIStyles {

    public static final Color PRIMARY_GREEN = new Color(34, 139, 34);
    public static final Color WARNING_ORANGE = new Color(230, 140, 20);
    public static final Color DANGER_RED = new Color(200, 50, 50);

    public static final Color BACKGROUND = new Color(245, 248, 245);
    public static final Color SIDEBAR = new Color(230, 240, 230);
    public static final Color CARD_BORDER = new Color(210, 225, 210);

    private static final Color PRIMARY_GREEN_HOVER = new Color(28, 120, 28);
    private static final Color PRIMARY_GREEN_PRESSED = new Color(20, 95, 20);
    private static final Color SUBTITLE_GREY = new Color(105, 105, 105);
    private static final Color NAV_ACTIVE = new Color(205, 232, 205);
    private static final Color NAV_BORDER = new Color(150, 165, 150);
    private static final Color BAR_EMPTY = new Color(228, 234, 228);

    private UIStyles() {
    }

    /**
     * Makes a button solid green with white bold text.
     * macOS Aqua ignores setBackground() unless the button is opaque and its Aqua border is
     * replaced, so we swap in a plain padding border and paint the background ourselves.
     */
    public static void stylePrimaryButton(JButton button) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.setBackground(PRIMARY_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Without the Aqua border there is no built-in hover/press feedback, so darken the green instead
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(e -> {
            ButtonModel model = button.getModel();
            if (model.isPressed()) {
                button.setBackground(PRIMARY_GREEN_PRESSED);
            } else if (model.isRollover()) {
                button.setBackground(PRIMARY_GREEN_HOVER);
            } else {
                button.setBackground(PRIMARY_GREEN);
            }
        });
    }

    /** White button for less important actions (Delete, Edit, Back...). */
    public static void styleSecondaryButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
    }

    /**
     * Sidebar button. The button of the screen that is open is shown in light green.
     * Like stylePrimaryButton, it uses its own border so the background colour
     * also shows on macOS.
     */
    public static void styleNavButton(JButton button, boolean active) {
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(NAV_BORDER, 1));
        button.setBackground(active ? NAV_ACTIVE : Color.WHITE);
        button.setForeground(active ? PRIMARY_GREEN_PRESSED : Color.BLACK);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Builds a page header: title with a small grey subtitle stacked on the left,
     * action buttons on the right.
     */
    public static JPanel createPageHeader(String title, String subtitle, JComponent... actions) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(PRIMARY_GREEN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(SUBTITLE_GREY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitleLabel);

        // GridBagLayout keeps the buttons in one row, vertically centred against the title block
        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 0, 0);
        for (JComponent action : actions) {
            actionPanel.add(action, gbc);
        }

        // Title goes in CENTER (not WEST): CENTER only gets the width left over after the buttons,
        // so a long title is cut short with "..." instead of running underneath them.
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(titleBlock, BorderLayout.CENTER);
        headerPanel.add(actionPanel, BorderLayout.EAST);
        return headerPanel;
    }

    /** Thin green outline with padding: the white "card" look used on the dashboard. */
    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    /** Titled outline with padding, used around forms. */
    public static Border createFormBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(25);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Prepares a progress bar whose colour we can change.
     * The macOS look draws its own blue bar and ignores setForeground(), so we
     * switch the bar to Swing's basic drawing, which uses our colour on every system.
     */
    public static void styleProgressBar(JProgressBar bar) {
        bar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionBackground() {
                return Color.DARK_GRAY; // colour of the "53%" text over the empty part
            }

            @Override
            protected Color getSelectionForeground() {
                return Color.WHITE; // colour of the text over the filled part
            }
        });
        bar.setOpaque(true); // so the empty part is painted in our background colour
        bar.setStringPainted(true);
        bar.setFont(new Font("Arial", Font.BOLD, 11));
        bar.setBackground(BAR_EMPTY);
        bar.setForeground(PRIMARY_GREEN);
        bar.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
    }

    /**
     * The traffic-light rule for a daily limit:
     * green while below 80% of the goal, orange from 80% to 100%, red when over 100%.
     */
    public static Color getProgressColor(double percent) {
        if (percent > 100) {
            return DANGER_RED;
        } else if (percent >= 80) {
            return WARNING_ORANGE;
        }
        return PRIMARY_GREEN;
    }

    /** Shows 120.0 as "120" and 46.53 as "46.5". */
    public static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    // ---------- message dialogs ----------

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "NutriTrack", JOptionPane.INFORMATION_MESSAGE);
    }

    /** For mistakes the user can fix themselves, such as a wrong input. */
    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * For problems the user cannot fix (database down, file not writable).
     * The user sees only the friendly message. The technical details go to the
     * console, where a developer can read them.
     */
    public static void showError(Component parent, String message, Exception ex) {
        System.err.println("NutriTrack error: " + ex);
        if (ex.getCause() != null) {
            System.err.println("  caused by: " + ex.getCause());
        }
        JOptionPane.showMessageDialog(parent, message, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    /** Our own exceptions already carry a message that is safe to show. */
    public static void showError(Component parent, NutriTrackException ex) {
        showError(parent, ex.getMessage(), ex);
    }
}
