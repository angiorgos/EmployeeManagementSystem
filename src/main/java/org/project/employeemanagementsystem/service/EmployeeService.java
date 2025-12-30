package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // Constructor Injection (Βέλτιστη πρακτική αντί για @Autowired στο field)
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // --- 1. ΑΝΑΓΝΩΣΗ (READ) ---

    // Επιστρέφει ΟΛΟΥΣ τους υπαλλήλους (Ενεργούς και μη) - Για τον κεντρικό πίνακα
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Επιστρέφει ΜΟΝΟ τους ΕΝΕΡΓΟΥΣ (χωρίς ημερομηνία εξόδου) - Για Dropdowns/ComboBoxes
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByExitDateIsNull();
    }

    // Εύρεση με βάση το ID
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    // Αναζήτηση με βάση το επώνυμο (αν είναι κενό, επιστρέφει όλους)
    public List<Employee> searchEmployees(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.findByLastNameContainingIgnoreCase(lastName);
    }

    // --- 2. ΕΓΓΡΑΦΗ / ΕΝΗΜΕΡΩΣΗ (CREATE / UPDATE) ---

    public void saveEmployee(Employee employee) {
        // Έλεγχος: Αν είναι νέος υπάλληλος (id == null) και το email υπάρχει ήδη
        if (employee.getId() == null && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email already in use!");
        }

        // Αν ορίζουμε Exit Date, θεωρείται ότι αποχωρεί (μπορείς να βάλεις extra logic εδώ αν θες)
        employeeRepository.save(employee);
    }

    // --- 3. ΔΙΑΓΡΑΦΗ (DELETE) ---

    public void deleteEmployee(Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Employee with id " + id + " not found!");
        }
    }
}