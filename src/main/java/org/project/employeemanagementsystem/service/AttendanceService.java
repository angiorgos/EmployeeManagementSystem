package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee; // <-- Χρειαζόμαστε αυτό
import org.project.employeemanagementsystem.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration; // <-- Για τον υπολογισμό ώρας
import java.time.LocalDate;
import java.time.YearMonth; // <-- Για να βρούμε αρχή/τέλος μήνα
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Attendance> getAllAttendanceRecords() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getAttendanceByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public void saveAttendance(Attendance attendance) {
        attendanceRepository.save(attendance);
    }

    public void deleteAttendance(Attendance attendance) {
        attendanceRepository.delete(attendance);
    }

    // ================================================================
    // ΝΕΑ ΜΕΘΟΔΟΣ ΓΙΑ ΤΟ PAYROLL (ΥΠΟΛΟΓΙΣΜΟΣ ΩΡΩΝ)
    // ================================================================
    public double calculateTotalHoursWorked(Employee employee, LocalDate payrollDate) {

        // 1. Βρίσκουμε την 1η και την τελευταία μέρα του μήνα πληρωμής
        YearMonth yearMonth = YearMonth.from(payrollDate);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 2. Ζητάμε από τη βάση τις εγγραφές (Java style, όχι SQL)
        List<Attendance> monthlyRecords = attendanceRepository.findByEmployeeAndDateBetween(employee, startOfMonth, endOfMonth);

        long totalMinutes = 0;

        // 3. Τρέχουμε τη λίστα και προσθέτουμε τη διάρκεια
        for (Attendance att : monthlyRecords) {
            // Υπολογίζουμε ΜΟΝΟ αν έχει χτυπήσει και είσοδο και έξοδο
            if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                // Το Duration.between υπολογίζει αυτόματα τη διαφορά
                totalMinutes += Duration.between(att.getCheckInTime(), att.getCheckOutTime()).toMinutes();
            }
        }

        // 4. Επιστρέφουμε ώρες (τα λεπτά δια 60)
        return totalMinutes / 60.0;
    }
    public double calculateSundayHours(Employee employee, LocalDate payrollDate) {
        YearMonth yearMonth = YearMonth.from(payrollDate);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<Attendance> monthlyRecords = attendanceRepository.findByEmployeeAndDateBetween(employee, startOfMonth, endOfMonth);

        long sundayMinutes = 0;

        for (Attendance att : monthlyRecords) {
            // Έλεγχος: Είναι Κυριακή;
            if (att.getDate().getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                    sundayMinutes += Duration.between(att.getCheckInTime(), att.getCheckOutTime()).toMinutes();
                }
            }
        }
        return sundayMinutes / 60.0;
    }
}