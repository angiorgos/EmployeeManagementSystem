package org.project.employeemanagementsystem.service;

import jakarta.transaction.Transactional;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ===================== READ =====================

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByExitDateIsNull();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    // ===================== CREATE / UPDATE =====================

    public void saveEmployee(Employee employee) {

        employeeRepository.findByEmail(employee.getEmail())
                .ifPresent(existing -> {
                    if (employee.getId() == null ||
                            !existing.getId().equals(employee.getId())) {
                        throw new IllegalArgumentException("Email already in use!");
                    }
                });

        employeeRepository.save(employee);
    }

    // ===================== DELETE =====================

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalArgumentException("Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    public void softDeleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Αν έχει ήδη φύγει, πετάμε μήνυμα ή απλά επιστρέφουμε
        if (employee.getExitDate() != null) {
            throw new IllegalStateException("Employee is already inactive since " + employee.getExitDate());
        }

        // Θέτουμε ημερομηνία αποχώρησης τη σημερινή
        employee.setExitDate(java.time.LocalDate.now());

        // Αποθηκεύουμε την αλλαγή
        employeeRepository.save(employee);
    }


    public void rehireEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getExitDate() == null) {
            throw new IllegalStateException("Employee is already active!");
        }

        // Καθαρίζουμε την ημερομηνία εξόδου -> Ο υπάλληλος γίνεται ξανά ενεργός
        employee.setExitDate(null);

        // Προαιρετικά: Μπορείς να αλλάξεις και το HireDate στη σημερινή μέρα
        // employee.setHireDate(java.time.LocalDate.now());

        employeeRepository.save(employee);
    }
}
