package org.project.employeemanagementsystem.util;

import javafx.application.Application;
import javafx.stage.Stage;
import org.project.employeemanagementsystem.EmployeeManagementSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(EmployeeManagementSystemApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        Navigator navigator = springContext.getBean(Navigator.class);
        navigator.setMainStage(primaryStage);
        navigator.loadScene(Navigator.LOGIN_VIEW, "Σύστημα Διαχείρισης - Είσοδος");
    }

    @Override
    public void stop() {
        springContext.close();
    }
}