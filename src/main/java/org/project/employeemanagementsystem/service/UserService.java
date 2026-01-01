package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // <--- ΣΗΜΑΝΤΙΚΟ IMPORT
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // <--- Ο Μηχανισμός Κρυπτογράφησης

    // Constructor Injection (Πιο σωστό από το Field Injection)
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        // --- ΠΕΡΙΠΤΩΣΗ 1: ΝΕΟΣ ΧΡΗΣΤΗΣ (Insert) ---
        if (user.getId() == null) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists!");
            }
            // Κρυπτογράφηση του κωδικού πριν την αποθήκευση
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }

        // --- ΠΕΡΙΠΤΩΣΗ 2: ΕΠΕΞΕΡΓΑΣΙΑ (Update) ---
        else {
            // Βρίσκουμε τον παλιό χρήστη από τη βάση για να δούμε τον παλιό κωδικό
            User existingUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found during update!"));

            // Ελέγχουμε αν ο χρήστης έγραψε ΝΕΟ κωδικό στη φόρμα
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                // Αν έγραψε κάτι, το κρυπτογραφούμε
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                // Αν το άφησε κενό ή null, κρατάμε τον παλιό κωδικό (που είναι ήδη encrypted)
                user.setPassword(existingUser.getPassword());
            }

            return userRepository.save(user);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void deleteUser(User user) {
        if (userRepository.existsById(user.getId())) {
            userRepository.delete(user);
        } else {
            throw new RuntimeException("User not found!");
        }
    }
}