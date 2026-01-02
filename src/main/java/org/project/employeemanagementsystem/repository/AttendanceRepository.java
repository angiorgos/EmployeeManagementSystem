package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByEmployeeId(Long employeeId);

    List<Attendance> findByDate(LocalDate date);

    // Βρίσκει όλες τις εγγραφές ενός υπαλλήλου σε ένα εύρος ημερομηνιών (για μισθοδοσία)
    List<Attendance> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate);

    // Βρίσκει αν υπάρχει ήδη εγγραφή για τον υπάλληλο τη συγκεκριμένη μέρα (για check-in/out)
    Optional<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);


}