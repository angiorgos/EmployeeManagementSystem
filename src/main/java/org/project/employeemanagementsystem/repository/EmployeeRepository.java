package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByLastNameContainingIgnoreCase(String lastName);
    List<Employee> findByExitDateIsNull();
    boolean existsByEmail(String email);
    //Μετράει πόσοι υπάλληλοι ανήκουν σε ένα τμήμα
    long countByDepartmentId(Long departmentId);
}