package org.project.employeemanagementsystem;

import javafx.application.Application;
import org.project.employeemanagementsystem.util.JavaFxApplication; // Θα το φτιάξουμε στο βήμα 2
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmployeeManagementSystemApplication {

    public static void main(String[] args) {
        // Αντί να τρέξει το Spring απευθείας, λέμε στο JavaFX να ξεκινήσει
        // και του δίνουμε την κλάση που διαχειρίζεται τα γραφικά (JavaFxApplication)
        Application.launch(JavaFxApplication.class, args);
    }


}