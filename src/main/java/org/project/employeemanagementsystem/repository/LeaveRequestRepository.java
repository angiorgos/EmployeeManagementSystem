package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Find all requests for a specific employee
    List<LeaveRequest> findByEmployee(Employee employee);

    // Find requests by status (e.g., all PENDING requests for the Admin to see)
    List<LeaveRequest> findByStatus(LeaveStatus status);

    // This is the "Magic" query to calculate used days
    // It sums the days between startDate and endDate for APPROVED requests of a specific type
    @Query("SELECT SUM(DATEDIFF(l.endDate, l.startDate) + 1) FROM LeaveRequest l " +
            "WHERE l.employee = :employee " +
            "AND l.leaveType = :leaveType " +
            "AND l.status.name = 'APPROVED' " +
            "AND YEAR(l.startDate) = :year")
    Integer countUsedDays(@Param("employee") Employee employee,
                          @Param("leaveType") LeaveType leaveType,
                          @Param("year") int year);
}