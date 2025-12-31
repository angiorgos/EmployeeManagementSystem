package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.repository.DepartmentRepository;
import org.project.employeemanagementsystem.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public void saveDepartment(Department department) {
        if (department.getId() == null && departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Department already exists!");
        }
        departmentRepository.save(department);
    }


    public void deleteDepartment(Department department) {
        long employeeCount = employeeRepository.countByDepartmentId(department.getId());

        if (employeeCount > 0) {
            throw new RuntimeException("Cannot delete Department. It has " + employeeCount + " employees!");
        }

        departmentRepository.delete(department);
    }
}