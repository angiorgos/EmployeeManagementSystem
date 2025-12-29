package org.project.employeemanagementsystem.config;

import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.RoleRepository;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            // 1. Δημιουργία Ρόλου ADMIN (αν δεν υπάρχει)
            Role adminRole = roleRepository.findByName("ADMIN");
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ADMIN");
                adminRole.setDescription("Administrator with full access");
                roleRepository.save(adminRole);
                System.out.println("✅ Role ADMIN created");
            }

            // 2. Δημιουργία Χρήστη ADMIN
            // Χρησιμοποιούμε .orElse(null) για να μην έχουμε σφάλμα με το Optional
            User admin = userRepository.findByUsername("admin").orElse(null);

            if (admin == null) {
                // ΔΗΜΙΟΥΡΓΙΑ ΝΕΟΥ
                admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("1234")); // Κρυπτογράφηση
                admin.setRole(adminRole);

                // Δεν πειράζουμε Employee/ExitDate.
                // Η μέθοδος isActive() στο User επιστρέφει true αν employee == null.

                userRepository.save(admin);
                System.out.println("✅ User 'admin' created with password '1234'");
            } else {
                // UPDATE (Αν υπάρχει ήδη, επαναφέρουμε τον κωδικό για σιγουριά)
                admin.setPassword(passwordEncoder.encode("1234"));
                userRepository.save(admin);
                System.out.println("✅ User 'admin' exists - Password reset to '1234'");
            }
        };
    }
}