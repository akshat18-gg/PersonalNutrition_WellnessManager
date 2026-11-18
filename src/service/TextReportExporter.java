package service;

import model.DailySummary;
import model.Meal;
import model.MealItem;
import model.NutritionGoal;
import model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Saves the daily report as a plain text file that is easy to read or print.
 * It EXTENDS AbstractReportExporter, which already opens and closes the file,
 * so this class only decides what the text looks like.
 */
public class TextReportExporter extends AbstractReportExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public String getFormatName() {
        return "Text file (.txt)";
    }

    @Override
    protected String getFileExtension() {
        return "txt";
    }

    @Override
    protected void writeReport(BufferedWriter writer, User user, DailySummary summary,
                               NutritionGoal goal, List<Meal> meals) throws IOException {
        writeLine(writer, "NutriTrack - Daily Report");
        writeLine(writer, "User : " + user.getName());
        writeLine(writer, "Date : " + summary.getDate().format(DATE_FORMAT));
        writeLine(writer, "----------------------------------------");
        writeLine(writer, "");

        writeLine(writer, "SUMMARY (consumed / goal)");
        writeLine(writer, String.format("Calories : %.0f / %d kcal", summary.getCalories(), goal.getTargetCalories()));
        writeLine(writer, String.format("Protein  : %.1f / %.0f g", summary.getProtein(), goal.getTargetProtein()));
        writeLine(writer, String.format("Carbs    : %.1f / %.0f g", summary.getCarbs(), goal.getTargetCarbs()));
        writeLine(writer, String.format("Fat      : %.1f / %.0f g", summary.getFat(), goal.getTargetFat()));
        writeLine(writer, String.format("Water    : %d / %d ml", summary.getWaterMl(), goal.getTargetWater()));
        writeLine(writer, "");

        writeLine(writer, "MEALS");
        if (meals.isEmpty()) {
            writeLine(writer, "No meals logged on this day.");
        }
        for (Meal meal : meals) {
            writeLine(writer, String.format("%s (%.0f kcal)", meal.getMealType(), meal.getTotalCalories()));
            for (MealItem item : meal.getItems()) {
                writeLine(writer, String.format("  - %s, %.0f g, %.0f kcal",
                        item.getFood().getFoodName(), item.getQuantity(), item.getCalories()));
            }
        }
    }

    private void writeLine(BufferedWriter writer, String text) throws IOException {
        writer.write(text);
        writer.newLine(); // the correct line ending for Windows or macOS
    }
}
