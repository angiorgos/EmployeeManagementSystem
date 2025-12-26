package org.project.employeemanagementsystem;

import org.springframework.boot.SpringApplication;
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
    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("1234");
                admin.setRole("ADMIN");

                userRepository.save(admin);
                System.out.println("------------------------------------------------");
                System.out.println("ADMIN USER CREATED: Username: admin | Pass: 1234");
                System.out.println("------------------------------------------------");
            }
        };
    }
}
