package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LeaveStatusRepository extends JpaRepository<LeaveStatus, Long> {
    // Βρίσκει status αγνοώντας πεζά/κεφαλαία
    Optional<LeaveStatus> findByNameIgnoreCase(String name);
}