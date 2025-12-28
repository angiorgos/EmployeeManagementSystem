package org.project.employeemanagementsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Component
public class Navigator {

    //PATHS
    public static final String LOGIN_VIEW      = "/fxml/login.fxml";
    public static final String DASHBOARD_VIEW  = "/fxml/dashboard.fxml";
    public static final String EMPLOYEES_VIEW  = "/fxml/employees.fxml";


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

    //NAVIGATION METHODS
    public void goToDashboard() { loadScene(DASHBOARD_VIEW, "Dashboard Overview"); }
    public void goToEmployees() { loadScene(EMPLOYEES_VIEW, "Employee Management"); }

    public void logout() {
        userSession.logout();
        loadScene(LOGIN_VIEW, "Login");
    }

    public void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            loader.setControllerFactory(context::getBean);

            Parent root = loader.load();
            Scene currentScene = mainStage.getScene();

            if (currentScene == null) {
                mainStage.setScene(new Scene(root));
            } else {
                currentScene.setRoot(root);
            }

            mainStage.setTitle("EMS App - " + title);


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


    private void configureLoginWindow() {
        mainStage.setMaximized(false);
        mainStage.setResizable(false);
        mainStage.setWidth(500);
        mainStage.setHeight(550);
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