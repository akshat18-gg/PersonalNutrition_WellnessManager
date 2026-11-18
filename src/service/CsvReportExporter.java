package service;

import model.DailySummary;
import model.Meal;
import model.MealItem;
import model.NutritionGoal;
import model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Saves the daily report as a CSV file (comma separated values),
 * which opens directly in Excel or Google Sheets.
 * Like TextReportExporter, it only writes the content; AbstractReportExporter
 * creates the folder and opens and closes the file.
 */
public class CsvReportExporter extends AbstractReportExporter {

    @Override
    public String getFormatName() {
        return "Spreadsheet (.csv)";
    }

    @Override
    protected String getFileExtension() {
        return "csv";
    }

    @Override
    protected void writeReport(BufferedWriter writer, User user, DailySummary summary,
                               NutritionGoal goal, List<Meal> meals) throws IOException {
        writeRow(writer, "NutriTrack Daily Report", user.getName(), summary.getDate().toString());
        writer.newLine();

        writeRow(writer, "Metric", "Consumed", "Goal", "Unit");
        writeRow(writer, "Calories", number(summary.getCalories()), number(goal.getTargetCalories()), "kcal");
        writeRow(writer, "Protein", number(summary.getProtein()), number(goal.getTargetProtein()), "g");
        writeRow(writer, "Carbs", number(summary.getCarbs()), number(goal.getTargetCarbs()), "g");
        writeRow(writer, "Fat", number(summary.getFat()), number(goal.getTargetFat()), "g");
        writeRow(writer, "Water", number(summary.getWaterMl()), number(goal.getTargetWater()), "ml");
        writer.newLine();

        writeRow(writer, "Meal", "Food", "Quantity (g)", "Calories (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)");
        for (Meal meal : meals) {
            for (MealItem item : meal.getItems()) {
                writeRow(writer, meal.getMealType(), item.getFood().getFoodName(),
                        number(item.getQuantity()), number(item.getCalories()),
                        number(item.getProtein()), number(item.getCarbs()), number(item.getFat()));
            }
        }
    }

    // Writes the cells of one row separated by commas
    private void writeRow(BufferedWriter writer, String... cells) throws IOException {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            row.append(escape(cells[i]));
        }
        writer.write(row.toString());
        writer.newLine();
    }

    // A cell that itself contains a comma or a quote must be wrapped in quotes,
    // otherwise a food called "Rice, fried" would be split into two columns.
    private String escape(String cell) {
        if (cell.contains(",") || cell.contains("\"")) {
            return "\"" + cell.replace("\"", "\"\"") + "\"";
        }
        return cell;
    }

    // Locale.US forces a dot as the decimal point. Some computers are set to use
    // a comma (12,5), which would break the columns of a CSV file.
    private String number(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
