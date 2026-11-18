package gui;

import model.DailySummary;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bar chart of calories per day, drawn by hand with Graphics2D (no chart library).
 *
 * Swing calls paintComponent() whenever the panel must be drawn (first show,
 * resize, repaint()). Everything is calculated from the panel's current width
 * and height, so the chart stretches with the window.
 */
public class CalorieChartPanel extends JPanel {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE dd");

    // Empty space kept around the chart for the axis labels
    private static final int LEFT = 60;
    private static final int RIGHT = 30;
    private static final int TOP = 30;
    private static final int BOTTOM = 40;

    // Distance between the horizontal grid lines, in kcal
    private static final int GRID_STEP = 500;

    private Map<LocalDate, DailySummary> days = new LinkedHashMap<LocalDate, DailySummary>();
    private int goalCalories;

    // Calculated at the start of every paint and used by the draw methods
    private int chartWidth;
    private int chartHeight;
    private int baseY;        // y pixel of the 0 kcal line
    private double maxValue;  // kcal value at the top of the chart

    public CalorieChartPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(UIStyles.CARD_BORDER, 1));
    }

    /** Gives the chart new data and asks Swing to draw it again. */
    public void setData(Map<LocalDate, DailySummary> days, int goalCalories) {
        this.days = days;
        this.goalCalories = goalCalories;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // paints the white background first
        if (days.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        // Smooth edges for lines and text
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        chartWidth = getWidth() - LEFT - RIGHT;
        chartHeight = getHeight() - TOP - BOTTOM;
        baseY = TOP + chartHeight;
        maxValue = findMaxValue();

        drawGrid(g2);
        drawBars(g2);
        drawGoalLine(g2);
    }

    /** The chart must be tall enough for the biggest bar AND the goal line, plus 15% head room. */
    private double findMaxValue() {
        double max = goalCalories;
        for (DailySummary day : days.values()) {
            max = Math.max(max, day.getCalories());
        }
        return Math.max(max, GRID_STEP) * 1.15;
    }

    /** Converts a kcal value into a y pixel. Pixels count from the TOP, so we subtract from baseY. */
    private int toY(double calories) {
        return baseY - (int) Math.round(calories / maxValue * chartHeight);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        FontMetrics metrics = g2.getFontMetrics();

        for (int value = 0; value <= maxValue; value += GRID_STEP) {
            int y = toY(value);
            g2.setColor(new Color(232, 236, 232));
            g2.drawLine(LEFT, y, LEFT + chartWidth, y);

            // Right-align the number against the axis
            String label = String.valueOf(value);
            g2.setColor(Color.GRAY);
            g2.drawString(label, LEFT - 8 - metrics.stringWidth(label), y + 4);
        }

        // The two axes
        g2.setColor(Color.GRAY);
        g2.drawLine(LEFT, TOP, LEFT, baseY);
        g2.drawLine(LEFT, baseY, LEFT + chartWidth, baseY);
        g2.drawString("kcal", LEFT - 8 - metrics.stringWidth("kcal"), TOP - 10);
    }

    private void drawBars(Graphics2D g2) {
        // Each day gets an equal slot; the bar fills the middle 55% of its slot
        int slotWidth = chartWidth / days.size();
        int barWidth = (int) (slotWidth * 0.55);
        LocalDate today = LocalDate.now();

        int index = 0;
        for (DailySummary day : days.values()) {
            int x = LEFT + index * slotWidth + (slotWidth - barWidth) / 2;
            int centerX = x + barWidth / 2;

            if (day.hasMeals()) {
                int y = toY(day.getCalories());

                // Same green / orange / red rule as the dashboard progress bars
                double percentOfGoal = goalCalories > 0 ? day.getCalories() / goalCalories * 100 : 0;
                g2.setColor(UIStyles.getProgressColor(percentOfGoal));
                g2.fillRect(x, y, barWidth, baseY - y);

                // Calories written above the bar
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                drawCentered(g2, String.format("%.0f", day.getCalories()), centerX, y - 6);
            } else {
                g2.setFont(new Font("Arial", Font.PLAIN, 11));
                g2.setColor(Color.GRAY);
                drawCentered(g2, "no meals", centerX, baseY - 8);
            }

            // Day name under the axis
            String dayLabel = day.getDate().equals(today) ? "Today" : day.getDate().format(DAY_FORMAT);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(Color.DARK_GRAY);
            drawCentered(g2, dayLabel, centerX, baseY + 18);

            index++;
        }
    }

    private void drawGoalLine(Graphics2D g2) {
        if (goalCalories <= 0) {
            return;
        }
        int y = toY(goalCalories);

        // BasicStroke with a dash pattern: 8 pixels of line, 6 pixels of gap
        Stroke normalStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
        g2.setColor(new Color(60, 60, 60));
        g2.drawLine(LEFT, y, LEFT + chartWidth, y);
        g2.setStroke(normalStroke); // put the normal line style back

        String label = "Goal " + goalCalories + " kcal";
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        int labelWidth = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, LEFT + chartWidth - labelWidth, y - 6);
    }

    // drawString starts at the LEFT of the text, so shift it back by half its width
    private void drawCentered(Graphics2D g2, String text, int centerX, int y) {
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, centerX - textWidth / 2, y);
    }
}
