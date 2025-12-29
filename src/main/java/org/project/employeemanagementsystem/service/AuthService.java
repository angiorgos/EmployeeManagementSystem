package org.project.employeemanagementsystem.service;

import jakarta.transaction.Transactional;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder; // <--- Import
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional // <--- ΠΡΟΣΘΗΚΗ 1: Βάλε αυτό για να είναι φρέσκια η σύνδεση με τη βάση
    public User authenticate(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            System.out.println("Login Failed: User not found: " + username);
            return null;
        }

        User user = userOptional.get();

        // Debug Prints
        System.out.println("   Checking User: " + user.getUsername());
        System.out.println("   DB Password Hash: " + user.getPassword());
        System.out.println("   Input Password: " + rawPassword);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            System.out.println("Login Failed: Wrong Password");
            return null;
        }

        if (!user.isActive()) {
            System.out.println("Login Failed: User is Inactive");
            return null;
        }

        System.out.println("Login Success!");
        return user;
    }
}