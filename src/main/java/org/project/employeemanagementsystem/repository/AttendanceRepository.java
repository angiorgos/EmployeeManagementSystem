package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // Βρίσκει όλες τις παρουσίες ενός υπαλλήλου
    List<Attendance> findByEmployeeId(Long employeeId);

    // Βρίσκει αν ο υπάλληλος έχει χτυπήσει κάρτα μια συγκεκριμένη μέρα (για να μην χτυπήσει 2 φορές)
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
}