package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    // Βρίσκει τύπο άδειας αγνοώντας πεζά/κεφαλαία
    Optional<LeaveType> findByNameIgnoreCase(String name);
}