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
    private final SystemLogService systemLogService; // <--- Νέο Dependency

    // Constructor Injection για όλα
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

        // --- ΠΕΡΙΠΤΩΣΗ 1: ΔΕΝ ΒΡΕΘΗΚΕ Ο ΧΡΗΣΤΗΣ ---
        if (userOptional.isEmpty()) {
            System.out.println("Login Failed: User not found: " + username);

            // Καταγραφή αποτυχίας (Χωρίς User object, απλά κείμενο)
            systemLogService.log("LOGIN_FAILED", "User not found: " + username);
            return null;
        }

        User user = userOptional.get();

        // Debug Prints (Μπορείς να τα κρατήσεις ή να τα σβήσεις)
        System.out.println("   Checking User: " + user.getUsername());
        // System.out.println("   DB Password Hash: " + user.getPassword()); // Καλύτερα να μην τυπώνουμε hashes στα logs παραγωγής
        // System.out.println("   Input Password: " + rawPassword);

        // --- ΠΕΡΙΠΤΩΣΗ 2: ΛΑΘΟΣ ΚΩΔΙΚΟΣ ---
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            System.out.println("Login Failed: Wrong Password");

            // Καταγραφή αποτυχίας
            systemLogService.log("LOGIN_FAILED", "Wrong password for user: " + username);
            return null;
        }

        // --- ΠΕΡΙΠΤΩΣΗ 3: ΑΝΕΝΕΡΓΟΣ ΧΡΗΣΤΗΣ ---
        // (Υποθέτω ότι έχεις μέθοδο isActive() στο User model)
        try {
            if (!user.isActive()) {
                System.out.println("Login Failed: User is Inactive");
                systemLogService.log("LOGIN_FAILED", "Inactive account attempted login: " + username);
                return null;
            }
        } catch (Exception e) {
            // Αν δεν υπάρχει η μέθοδος isActive, αγνόησε το block
        }

        // --- ΠΕΡΙΠΤΩΣΗ 4: ΕΠΙΤΥΧΙΑ ---
        System.out.println("Login Success!");

        // Καταγραφή Επιτυχίας!
        // Χρησιμοποιούμε τη μέθοδο που δέχεται User object για να συνδεθεί σωστά στο log
        systemLogService.log("LOGIN_SUCCESS", user);

        return user;
    }
}