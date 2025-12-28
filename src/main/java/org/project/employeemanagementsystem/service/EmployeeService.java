package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    //ΑΠΟΘΗΚΕΥΣΗ (Create / Update)
    public void saveEmployee(Employee employee) {
        //Έλεγχος αν υπάρχει ήδη το email
        if (employee.getId() == null && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Το email αυτό χρησιμοποιείται ήδη!");
        }
        employeeRepository.save(employee);
    }

    // ΑΝΑΓΝΩΣΗ ΟΛΩΝ
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    //ΑΝΑΖΗΤΗΣΗ
    public List<Employee> searchEmployees(String lastName) {
        if (lastName == null || lastName.isEmpty()) {
            return employeeRepository.findAll(); // Αν δεν έγραψε τίποτα, φέρτα όλα
        }
        return employeeRepository.findByLastNameContainingIgnoreCase(lastName);
    }

    //ΔΙΑΓΡΑΦΗ
    public void deleteEmployee(Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Ο υπάλληλος δεν βρέθηκε!");
        }
    }

    //ΕΥΡΕΣΗ ΕΝΟΣ
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }
}