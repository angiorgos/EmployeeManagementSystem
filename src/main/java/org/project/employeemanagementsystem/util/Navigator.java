package org.project.employeemanagementsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Navigator {

    public static String CURRENT_VIEW = "";

    //FXML PATHS
    public static final String LOGIN_VIEW       = "/fxml/login.fxml";
    public static final String DASHBOARD_VIEW = "/fxml/dashboard.fxml";

    // Workforce
    public static final String EMPLOYEES_VIEW   = "/fxml/employees.fxml";
    public static final String DEPARTMENTS_VIEW = "/fxml/departments.fxml";
    public static final String USERS_VIEW       = "/fxml/users.fxml";

    // Time & Attendance
    public static final String SCHEDULE_VIEW    = "/fxml/schedule.fxml";
    public static final String ATTENDANCE_VIEW  = "/fxml/attendance.fxml";
    public static final String HOLIDAYS_VIEW    = "/fxml/holidays.fxml";
    public static final String LEAVES_VIEW      = "/fxml/leaves.fxml";

    // Financials
    public static final String PAYROLL_VIEW     = "/fxml/payroll.fxml";

    // System
    public static final String LOGS_VIEW        = "/fxml/logs.fxml";


    //DEPENDENCIES
    private final ApplicationContext context;
    private final UserSession userSession;
    private Stage mainStage;

    // Constructor Injection
    public Navigator(ApplicationContext context, UserSession userSession) {
        this.context = context;
        this.userSession = userSession;
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public Stage getMainStage() {
        return mainStage;
    }

    //ΜΕΘΟΔΟΙ ΠΛΟΗΓΗΣΗΣ

    // Login & Dashboard
    public void goToLogin() {
        loadScene(LOGIN_VIEW, "Login");
    }

    public void goToDashboard() {
        loadScene(DASHBOARD_VIEW, "Dashboard - Overview");
    }

    // Workforce Methods
    public void goToEmployees() {
        loadScene(EMPLOYEES_VIEW, "Employee Management");
    }

    public void goToDepartments() {
        loadScene(DEPARTMENTS_VIEW, "Department Management");
    }

    public void goToUsers() {
        loadScene(USERS_VIEW, "System Users Management");
    }

    // Time & Attendance Methods
    public void goToSchedule() {
        loadScene(SCHEDULE_VIEW, "Work Schedule");
    }

    public void goToAttendance() {
        loadScene(ATTENDANCE_VIEW, "Attendance Tracker");
    }

    public void goToHolidays() {
        loadScene(HOLIDAYS_VIEW, "Holiday Management");
    }

    public void goToLeaveTypes() {

    }

    public void goToLeaves() {
        loadScene(LEAVES_VIEW, "Leave Management");
    }

    // Financials Methods
    public void goToPayroll() {
        loadScene(PAYROLL_VIEW, "Payroll System");
    }

    // System Methods
    public void goToLogs() {
        loadScene(LOGS_VIEW, "System Logs & Audit");
    }

    // Logout Helper
    public void logout() {
        userSession.logout();
        goToLogin();
    }



    //LOADING LOGIC
    public void loadScene(String fxmlPath, String title) {
        try {
            CURRENT_VIEW = fxmlPath;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Scene currentScene = mainStage.getScene();

            // Αν αλλάζουμε από Dashboard σε Login, καλύτερα να φτιάχνουμε νέα Scene
            // για να καθαρίζουν τυχόν styles/sizes που έμειναν.
            if (currentScene == null || fxmlPath.equals(LOGIN_VIEW)) {
                mainStage.setScene(new Scene(root));
            } else {
                currentScene.setRoot(root);
            }

            mainStage.setTitle("EMS App - " + title);

            // ΕΛΕΓΧΟΣ: Ποια οθόνη είναι για να φτιάξουμε το μέγεθος
            if (fxmlPath.equals(LOGIN_VIEW)) {
                configureLoginWindow();
            } else {
                configureMainWindow();
            }

            mainStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ΣΦΑΛΜΑ: Δεν βρέθηκε το αρχείο " + fxmlPath);
        }
    }

    //WINDOW CONFIGURATION HELPERS

    private void configureLoginWindow() {
        // 1. Απελευθερώνουμε τα ελάχιστα όρια που έβαλε το Dashboard
        mainStage.setMinWidth(0);
        mainStage.setMinHeight(0);

        // 2. Ρυθμίζουμε το παράθυρο για Login
        mainStage.setMaximized(false);
        mainStage.setResizable(false);

        // 3. Ορίζουμε το ακριβές μέγεθος
        mainStage.setWidth(500);
        mainStage.setHeight(550);

        // 4. Κεντράρισμα (ΠΡΟΣΟΧΗ: Πρέπει να γίνει αφού οριστούν τα μεγέθη)
        mainStage.centerOnScreen();
    }

    private void configureMainWindow() {
        mainStage.setResizable(true);
        mainStage.setMinWidth(1000);
        mainStage.setMinHeight(700);

        if (!mainStage.isMaximized()) {
            mainStage.setWidth(1400);
            mainStage.setHeight(900);
            mainStage.centerOnScreen();
        }
    }

    public void maximize() {
        if (mainStage != null) {
            mainStage.setMaximized(true);
        }
    }
}