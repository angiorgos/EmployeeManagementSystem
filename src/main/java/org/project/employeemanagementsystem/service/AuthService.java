package org.project.employeemanagementsystem.service;

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

    public User authenticate(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        //ΕΛΕΓΧΟΣ ΚΩΔΙΚΟΥ (Με κρυπτογράφηση)
        // matches(κωδικός_που_έγραψε, κωδικός_στη_βάση)
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return null; // Λάθος κωδικός
        }

        //Έλεγχος αν είναι ενεργός
        if (!user.isActive()) {
            return null; 
        }

        return user;
    }
}