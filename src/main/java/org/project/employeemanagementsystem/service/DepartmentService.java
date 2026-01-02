package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.repository.DepartmentRepository;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SystemLogService systemLogService; // <--- Προσθήκη για Audit Log

    // Constructor Injection
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
        boolean isNew = (department.getId() == null);

        // Έλεγχος για διπλότυπο όνομα μόνο αν είναι νέο τμήμα
        if (isNew && departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Department already exists!");
        }

        Department savedDept = departmentRepository.save(department);

        // --- AUDIT LOG ---
        String action = isNew ? "CREATE_DEPARTMENT" : "UPDATE_DEPARTMENT";
        systemLogService.log(action, "Department Name: " + savedDept.getName());
    }

    @Transactional
    public void deleteDepartment(Department department) {
        long employeeCount = employeeRepository.countByDepartmentId(department.getId());

        if (employeeCount > 0) {
            // Log failed attempt if you want (Optional)
            // systemLogService.log("DELETE_DEPARTMENT_FAILED", "Tried to delete " + department.getName() + " but had employees.");
            throw new RuntimeException("Cannot delete Department. It has " + employeeCount + " employees!");
        }

        String deptName = department.getName(); // Κρατάμε το όνομα πριν τη διαγραφή για το log
        departmentRepository.delete(department);

        // --- AUDIT LOG ---
        systemLogService.log("DELETE_DEPARTMENT", "Deleted Department: " + deptName);
    }
}