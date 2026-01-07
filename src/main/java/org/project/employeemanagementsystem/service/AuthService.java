package org.project.employeemanagementsystem.service;

import jakarta.transaction.Transactional; // Ή org.springframework.transaction.annotation.Transactional ανάλογα το setup σου
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;


    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;
    }

    @Transactional
    public User authenticate(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            System.out.println("Login Failed: User not found: " + username);

            systemLogService.log("LOGIN_FAILED", "User not found: " + username);
            return null;
        }

        User user = userOptional.get();

        // Debug Prints
        System.out.println("   Checking User: " + user.getUsername());

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            System.out.println("Login Failed: Wrong Password");

            systemLogService.log("LOGIN_FAILED", "Wrong password for user: " + username);
            return null;
        }

        try {
            if (!user.isActive()) {
                System.out.println("Login Failed: User is Inactive");
                systemLogService.log("LOGIN_FAILED", "Inactive account attempted login: " + username);
                return null;
            }
        } catch (Exception e) {
        }

        System.out.println("Login Success!");

        systemLogService.log("LOGIN_SUCCESS", user);

        return user;
    }
}