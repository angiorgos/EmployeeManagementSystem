package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

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
        departmentRepository.delete(department);
    }
}