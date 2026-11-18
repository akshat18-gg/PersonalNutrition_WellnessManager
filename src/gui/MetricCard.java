package gui;

import javax.swing.*;
import java.awt.*;

/**
 * One white card on the dashboard: title, "consumed / goal", a progress bar
 * and a short note. The dashboard creates five of these (calories, protein,
 * carbs, fat, water) instead of repeating the same layout code five times.
 */
public class MetricCard extends JPanel {

    private final String unit;
    // true  = a limit we should not go over (calories, protein, carbs, fat)
    // false = a target we want to reach (water), so going over is fine
    private final boolean isLimit;

    private final JLabel valueLabel = new JLabel("-");
    private final JLabel noteLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    public MetricCard(String title, String unit, boolean isLimit) {
        this.unit = unit;
        this.isLimit = isLimit;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(UIStyles.createCardBorder());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(Color.GRAY);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 19));
        valueLabel.setForeground(UIStyles.PRIMARY_GREEN);

        UIStyles.styleProgressBar(progressBar);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setPreferredSize(new Dimension(100, 20));

        noteLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        noteLabel.setForeground(Color.DARK_GRAY);

        // BoxLayout lines children up by their alignment, so all four must be LEFT
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(8));
        add(valueLabel);
        add(Box.createVerticalStrut(12));
        add(progressBar);
        add(Box.createVerticalStrut(10));
        add(noteLabel);
    }

    /** Shows today's amount against the goal and colours the bar. */
    public void update(double consumed, double target) {
        valueLabel.setText(String.format("%.0f / %.0f %s", consumed, target, unit));

        double percent = 0;
        if (target > 0) {
            percent = consumed / target * 100;
        }
        // The bar itself stops at 100, but the text still shows the real percentage (for example 120%)
        progressBar.setValue((int) Math.min(100, Math.round(percent)));
        progressBar.setString(Math.round(percent) + "%");

        Color color = UIStyles.PRIMARY_GREEN;
        if (isLimit) {
            color = UIStyles.getProgressColor(percent);
        }
        progressBar.setForeground(color);
        valueLabel.setForeground(color);

        double remaining = target - consumed;
        if (remaining > 0) {
            noteLabel.setText(String.format("Remaining: %.0f %s", remaining, unit));
        } else if (isLimit && remaining < 0) {
            noteLabel.setText(String.format("Over by %.0f %s", -remaining, unit));
        } else {
            noteLabel.setText("Goal reached");
        }
    }
}
