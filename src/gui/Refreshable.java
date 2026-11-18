package gui;

/**
 * A screen that can reload its data from the database.
 *
 * Every panel in the dashboard implements this interface. DashboardFrame
 * calls refreshData() whenever a screen is opened, without needing to know
 * which panel it is or what its own loading methods are called.
 */
public interface Refreshable {

    void refreshData();
}
