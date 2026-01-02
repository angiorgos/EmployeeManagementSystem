package org.project.employeemanagementsystem.service;

import jakarta.transaction.Transactional;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SystemLogService systemLogService; // <--- Προσθήκη Audit

    // Constructor Injection
    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, SystemLogService systemLogService) {
        this.employeeRepository = employeeRepository;
        this.systemLogService = systemLogService;
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
        // Έλεγχος αν είναι νέος υπάλληλος (πριν το save, γιατί μετά θα πάρει ID)
        boolean isNew = (employee.getId() == null);

        // Validation Email
        employeeRepository.findByEmail(employee.getEmail())
                .ifPresent(existing -> {
                    if (employee.getId() == null ||
                            !existing.getId().equals(employee.getId())) {
                        throw new IllegalArgumentException("Email already in use!");
                    }
                });

        Employee savedEmployee = employeeRepository.save(employee);

        // --- AUDIT LOG ---
        String action = isNew ? "CREATE_EMPLOYEE" : "UPDATE_EMPLOYEE";
        String details = "Employee: " + savedEmployee.getFirstName() + " " + savedEmployee.getLastName()
                + " (ID: " + savedEmployee.getId() + ")";

        systemLogService.log(action, details);
    }

    // ===================== DELETE =====================

    public void deleteEmployee(Long id) {
        // Βρίσκουμε τον υπάλληλο ΠΡΙΝ τη διαγραφή για να καταγράψουμε το όνομα
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        employeeRepository.delete(employee);

        // --- AUDIT LOG ---
        systemLogService.log("DELETE_EMPLOYEE", "Deleted Employee: " + fullName);
    }

    // ===================== SOFT DELETE (TERMINATION) =====================

    public void softDeleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getExitDate() != null) {
            throw new IllegalStateException("Employee is already inactive since " + employee.getExitDate());
        }

        employee.setExitDate(java.time.LocalDate.now());
        employeeRepository.save(employee);

        // --- AUDIT LOG ---
        systemLogService.log("TERMINATE_EMPLOYEE",
                "Soft Deleted (Exit Date Set): " + employee.getFirstName() + " " + employee.getLastName());
    }

    // ===================== REHIRE =====================

    public void rehireEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getExitDate() == null) {
            throw new IllegalStateException("Employee is already active!");
        }

        employee.setExitDate(null);
        // employee.setHireDate(java.time.LocalDate.now()); // Optional

        employeeRepository.save(employee);

        // --- AUDIT LOG ---
        systemLogService.log("REHIRE_EMPLOYEE",
                "Re-activated Employee: " + employee.getFirstName() + " " + employee.getLastName());
    }
}