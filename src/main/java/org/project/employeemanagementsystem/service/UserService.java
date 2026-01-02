package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.SystemLog; // <--- Χρειάζεται για τη λίστα logs
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.project.employeemanagementsystem.repository.SystemLogRepository; // <--- Χρειάζεται για διαγραφή logs
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    // 1. Δηλώνουμε όλα τα dependencies ως FINAL
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SystemLogRepository systemLogRepository;
    private final PasswordEncoder passwordEncoder;

    // 2. ΕΝΑΣ Constructor για όλα (Best Practice)
    @Autowired
    public UserService(UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       SystemLogRepository systemLogRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
        // --- ΠΕΡΙΠΤΩΣΗ 1: ΝΕΟΣ ΧΡΗΣΤΗΣ (Insert) ---
        if (user.getId() == null) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists!");
            }
            // Νέος χρήστης -> Κρυπτογράφηση
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }

        // --- ΠΕΡΙΠΤΩΣΗ 2: ΕΠΕΞΕΡΓΑΣΙΑ (Update) ---
        else {
            User existingUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found during update!"));

            // Ελέγχουμε αν ο χρήστης άλλαξε τον κωδικό
            if (user.getPassword() != null && !user.getPassword().equals(existingUser.getPassword())) {
                // Είναι διαφορετικός (raw text), άρα τον κρυπτογραφούμε
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                // Είναι ίδιος (hash) ή κενός, κρατάμε τον παλιό
                user.setPassword(existingUser.getPassword());
            }

            return userRepository.save(user);
        }
    }

    public void deleteUser(User user) {
        // Βήμα 1: Αποσύνδεση από Υπάλληλο (για να μην χτυπήσει FK constraint employees)
        if (user.getEmployee() != null) {
            Employee emp = user.getEmployee();
            emp.setUser(null);
            employeeRepository.save(emp);
        }

        // Βήμα 2: Διαγραφή Ιστορικού (για να μην χτυπήσει FK constraint system_logs)
        // Βεβαιώσου ότι στο SystemLogRepository έχεις τη μέθοδο findByUser(User user)
        List<SystemLog> logs = systemLogRepository.findByUser(user);
        systemLogRepository.deleteAll(logs);

        // Βήμα 3: Τελική Διαγραφή Χρήστη
        userRepository.delete(user);
    }
}