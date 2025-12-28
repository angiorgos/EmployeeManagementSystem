package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Custom query: Βρες υπαλλήλους με βάση το επίθετο (αγνοώντας πεζά/κεφαλαία)
    // Το Spring γράφει το SQL αυτόματα.
    List<Employee> findByLastNameContainingIgnoreCase(String lastName);

    // Custom query: Βρες με βάση το email (χρήσιμο για ελέγχους να μην διπλοεγγραφούν)
    boolean existsByEmail(String email);
}