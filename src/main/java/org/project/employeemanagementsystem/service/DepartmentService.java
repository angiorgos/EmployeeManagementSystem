package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.repository.DepartmentRepository;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SystemLogService systemLogService;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             SystemLogService systemLogService) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.systemLogService = systemLogService;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public void saveDepartment(Department department) {
        // 1. Έλεγχος αν υπάρχει ήδη Τμήμα με αυτό το όνομα
        // Υποθέτουμε ότι στο Repository έχεις: Department findByName(String name);
        Department existing = departmentRepository.findByName(department.getName());

        if (existing != null) {
            // Αν είναι νέο τμήμα (id == null) ΚΑΙ υπάρχει ήδη -> Λάθος
            // Ή αν κάνουμε Edit (id != null) αλλά το όνομα ανήκει σε ΑΛΛΟ id -> Λάθος
            if (department.getId() == null || !existing.getId().equals(department.getId())) {
                throw new RuntimeException("Department with name '" + department.getName() + "' already exists!");
            }
        }

        boolean isNew = (department.getId() == null);

        // Αποθήκευση
        Department savedDept = departmentRepository.save(department);

        // --- AUDIT LOG ---
        String action = isNew ? "CREATE_DEPARTMENT" : "UPDATE_DEPARTMENT";
        systemLogService.log(action, "Department Name: " + savedDept.getName());
    }

    @Transactional
    public void deleteDepartment(Department department) {
        long employeeCount = employeeRepository.countByDepartmentId(department.getId());

        if (employeeCount > 0) {
            throw new RuntimeException("Cannot delete Department. It has " + employeeCount + " employees!");
        }

        String deptName = department.getName();
        departmentRepository.delete(department);
        systemLogService.log("DELETE_DEPARTMENT", "Deleted Department: " + deptName);
    }
}