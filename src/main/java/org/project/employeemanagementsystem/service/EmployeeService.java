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
}
