package org.project.employeemanagementsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Ξεκινάει το Spring και συνδέεται στη βάση Render
        context = new SpringApplicationBuilder(EmployeeManagementSystemApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Φορτώνει το Login FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(context::getBean); // Συνδέει Spring με JavaFX Controllers

        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 300);

        stage.setTitle("EMS Login");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}