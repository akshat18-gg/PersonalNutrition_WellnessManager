package gui;

import dao.WaterDAO;
import dao.impl.WaterDAOImpl;
import exception.DatabaseException;
import service.NutritionService;

import javax.swing.*;
import java.time.LocalDate;

/**
 * A BACKGROUND THREAD that reminds the user to drink water.
 *
 * It starts when the user logs in. One minute later, and then every 30 minutes,
 * it checks today's water total. If the user is still below their water goal,
 * it shows a small reminder window. The app keeps working normally while this
 * thread sleeps and wakes in the background.
 *
 * Two threads are involved:
 *  - the reminder thread runs run(): it sleeps and reads the database;
 *  - Swing's Event Dispatch Thread (EDT) draws the reminder window.
 * Swing is not thread-safe, so the reminder thread never touches Swing itself.
 * It hands the work to the EDT with SwingUtilities.invokeLater().
 */
public class WaterReminder implements Runnable {

    // Short first delay so the reminder can be shown in a demo
    private static final long FIRST_CHECK_DELAY_MS = 60 * 1000L;      // 1 minute after login
    private static final long CHECK_INTERVAL_MS = 30 * 60 * 1000L;    // then every 30 minutes

    private final JFrame owner;
    private final int userId;
    private final WaterDAO waterDao = new WaterDAOImpl();
    private final NutritionService nutritionService = new NutritionService();

    // 'volatile': this flag is changed by the GUI thread (stop) and read by the
    // reminder thread (run). volatile makes sure the reminder thread sees the change.
    private volatile boolean running;
    private Thread thread;

    public WaterReminder(JFrame owner, int userId) {
        this.owner = owner;
        this.userId = userId;
    }

    /** Creates and starts the background thread. */
    public void start() {
        running = true;
        thread = new Thread(this, "water-reminder"); // 'this' is the Runnable to run
        thread.setDaemon(true); // a daemon thread does not keep the app alive after it closes
        thread.start();         // start() runs run() on the NEW thread (calling run() directly would not)
    }

    /** Stops the thread, for example on logout. */
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt(); // wakes the thread up if it is sleeping, so it ends at once
        }
    }

    /** The code that runs on the background thread. */
    @Override
    public void run() {
        try {
            Thread.sleep(FIRST_CHECK_DELAY_MS);
            while (running) {
                checkWater();
                Thread.sleep(CHECK_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            // stop() interrupted the sleep: we simply leave the loop and the thread ends
        }
    }

    /** Reads today's water from the database and, if needed, asks the EDT to show a reminder. */
    private void checkWater() {
        try {
            int drunkMl = waterDao.getTotalForDate(userId, LocalDate.now());
            int goalMl = nutritionService.getGoals(userId).getTargetWater();
            if (drunkMl >= goalMl) {
                return; // goal reached, no reminder needed
            }

            final String message = "Time for some water!\n\n"
                    + "Today so far: " + drunkMl + " ml of your " + goalMl + " ml goal.\n"
                    + "Still to go: " + (goalMl - drunkMl) + " ml.";

            // Hand the window work over to Swing's own thread
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (running) { // the user may have logged out in the meantime
                        showReminder(message);
                    }
                }
            });

        } catch (DatabaseException e) {
            // A missed reminder is not worth an error popup, so we only log it
            System.err.println("Water reminder skipped: " + e.getMessage());
        }
    }

    /** Runs on the EDT. A non-modal window, so the user can keep working while it is open. */
    private void showReminder(String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(owner, "Water Reminder");
        dialog.setModal(false);
        dialog.setVisible(true);
    }
}
