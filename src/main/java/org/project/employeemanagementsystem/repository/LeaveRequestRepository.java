package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    // Βρίσκει όλες τις άδειες ενός συγκεκριμένου υπαλλήλου
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    // Βρίσκει όλες τις άδειες που έχουν συγκεκριμένο Status
    List<LeaveRequest> findByStatusNameIgnoreCase(String statusName);
}