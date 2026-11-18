package service;

import model.DailySummary;
import model.Meal;
import model.NutritionGoal;
import model.User;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Something that can save a daily report to a file.
 *
 * The dashboard only knows this interface. Whether the file becomes plain
 * text or CSV depends on which implementation the user picked, and the
 * calling code is exactly the same for both (polymorphism).
 */
public interface ReportExporter {

    /** Name shown to the user when choosing a format, for example "Text file (.txt)". */
    String getFormatName();

    /**
     * Writes the report into the given folder and returns the file that was created.
     * Throws IOException if the file cannot be written.
     */
    File export(User user, DailySummary summary, NutritionGoal goal, List<Meal> meals, File folder)
            throws IOException;
}
