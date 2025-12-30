package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Βρες παρουσίες συγκεκριμένου υπαλλήλου
    List<Attendance> findByEmployeeId(Long employeeId);

    // Βρες παρουσίες συγκεκριμένης ημερομηνίας (π.χ. ποιοι ήρθαν σήμερα)
    List<Attendance> findByDate(LocalDate date);
}