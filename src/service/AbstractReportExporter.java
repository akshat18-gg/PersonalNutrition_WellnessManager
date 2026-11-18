package service;

import model.DailySummary;
import model.Meal;
import model.NutritionGoal;
import model.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Shared base class for the TXT and CSV exporters (an ABSTRACT class).
 *
 * Both exporters do the same steps: create the reports folder, choose a file
 * name, open the file, write the report, close the file. Only the "write the
 * report" step is different. So this class does the shared steps once, and
 * each subclass fills in only the part that is different.
 *
 * You cannot create an object of this class (new AbstractReportExporter() does
 * not compile), because writeReport() and getFileExtension() have no body here.
 */
public abstract class AbstractReportExporter implements ReportExporter {

    private static final String FILE_PREFIX = "nutritrack_report_";

    /** File extension without the dot, for example "txt". Each subclass gives its own. */
    protected abstract String getFileExtension();

    /** Writes the report itself. Each subclass decides how the file looks. */
    protected abstract void writeReport(BufferedWriter writer, User user, DailySummary summary,
                                        NutritionGoal goal, List<Meal> meals) throws IOException;

    /**
     * The shared steps. It is 'final' so a subclass cannot change the order of the steps;
     * it can only change the parts that are abstract above.
     */
    @Override
    public final File export(User user, DailySummary summary, NutritionGoal goal, List<Meal> meals, File folder)
            throws IOException {
        folder.mkdirs(); // creates the folder the first time; does nothing if it already exists
        File file = new File(folder, FILE_PREFIX + summary.getDate() + "." + getFileExtension());

        // BufferedWriter collects the text in memory and writes it in big chunks (faster).
        // try-with-resources closes the file even if a write fails half way.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writeReport(writer, user, summary, goal, meals);
        }
        return file;
    }
}
