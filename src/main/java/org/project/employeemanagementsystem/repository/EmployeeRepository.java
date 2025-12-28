package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.User; // Χρειάζεται για το findByUser
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Βρίσκει υπάλληλο με βάση το email
    Optional<Employee> findByEmail(String email);

    // Βρίσκει τον υπάλληλο που αντιστοιχεί σε έναν συγκεκριμένο User
    Optional<Employee> findByUser(User user);

    // Βρίσκει όλους τους υπαλλήλους ενός τμήματος
    List<Employee> findByDepartmentId(Long departmentId);
}