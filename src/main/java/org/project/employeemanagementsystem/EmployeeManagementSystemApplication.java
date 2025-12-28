package org.project.employeemanagementsystem;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
public class EmployeeManagementSystemApplication {

    public static void main(String[] args) {
        // Application.launch(JavaFxApplication.class, args);
        javafx.application.Application.launch(JavaFxApplication.class, args);
    }

}
