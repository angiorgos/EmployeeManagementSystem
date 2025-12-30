package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Βρες όλα τα προγράμματα ενός συγκεκριμένου υπαλλήλου
    List<Schedule> findByEmployeeId(Long employeeId);
}