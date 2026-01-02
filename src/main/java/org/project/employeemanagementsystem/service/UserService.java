package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.project.employeemanagementsystem.repository.SystemLogRepository;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SystemLogRepository systemLogRepository; // Χρειάζεται για καθαρισμό logs πριν το delete
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService; // <--- Προσθήκη για καταγραφή ενεργειών (Audit)

    @Autowired
    public UserService(UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       SystemLogRepository systemLogRepository,
                       PasswordEncoder passwordEncoder,
                       SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
        boolean isNew = (user.getId() == null);

        if (isNew) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists!");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        else {
            User existingUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found during update!"));

            if (user.getPassword() != null && !user.getPassword().equals(existingUser.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                user.setPassword(existingUser.getPassword());
            }
        }

        User savedUser = userRepository.save(user);

        // --- AUDIT LOG ---
        String action = isNew ? "CREATE_USER" : "UPDATE_USER";
        String roleName = (savedUser.getRole() != null) ? savedUser.getRole().getName() : "No Role";
        systemLogService.log(action, "Username: " + savedUser.getUsername() + " | Role: " + roleName);

        return savedUser;
    }

    public void deleteUser(User user) {
        String usernameToDelete = user.getUsername();

        // Βήμα 1: Αποσύνδεση από Υπάλληλο
        if (user.getEmployee() != null) {
            Employee emp = user.getEmployee();
            emp.setUser(null);
            employeeRepository.save(emp);
        }

        // Βήμα 2: Διαγραφή Ιστορικού (Cleanup)
        // Σβήνουμε τα logs που ΑΝΗΚΟΥΝ σε αυτόν τον χρήστη για να μην χτυπήσει FK
        List<SystemLog> logs = systemLogRepository.findByUser(user);
        systemLogRepository.deleteAll(logs);

        // Βήμα 3: Τελική Διαγραφή Χρήστη
        userRepository.delete(user);

        // --- AUDIT LOG ---
        // Καταγράφουμε ότι ΕΓΙΝΕ η διαγραφή (αυτό το log θα έχει user=null ή τον admin που το έκανε)
        systemLogService.log("DELETE_USER", "Deleted User account: " + usernameToDelete);
    }
}