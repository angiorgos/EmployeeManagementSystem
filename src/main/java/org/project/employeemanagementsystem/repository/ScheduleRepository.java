package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // ΓΙΑ ΤΟΝ ΕΛΕΓΧΟ OVERLAP (Διπλοβάρδια)
    // Βρίσκει τις βάρδιες του συγκεκριμένου υπαλλήλου για τη συγκεκριμένη μέρα.
    // Το Service θα τρέξει αυτή τη λίστα για να δει αν οι ώρες πέφτουν η μία πάνω στην άλλη.
    List<Schedule> findByEmployeeAndDate(Employee employee, LocalDate date);

    // ΓΙΑ ΤΟΝ ΕΛΕΓΧΟ ΕΒΔΟΜΑΔΙΑΙΟΥ ΟΡΙΟΥ (Weekly Limit)
    // Βρίσκει τις βάρδιες του υπαλλήλου μέσα σε ένα εύρος ημερομηνιών (StartOfWeek έως EndOfWeek).
    List<Schedule> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate);

    // ΓΙΑ ΤΟ UI (Το Πλέγμα/Grid)
    // Αυτό θα το χρειαστείς στον Controller για να δείξεις το πρόγραμμα ΟΛΩΝ των υπαλλήλων
    // για την εβδομάδα που επέλεξε ο χρήστης στο DatePicker.
    List<Schedule> findByDateBetween(LocalDate startDate, LocalDate endDate);
}