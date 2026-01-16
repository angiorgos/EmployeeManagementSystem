package org.project.employeemanagementsystem;

import javafx.application.Application;
import org.project.employeemanagementsystem.util.JavaFxApplication; // Θα το φτιάξουμε στο βήμα 2
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
//boot κλάση
public class EmployeeManagementSystemApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }


}