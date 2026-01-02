package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SystemLogService systemLogService; // <--- Προσθήκη για Audit Log

    // Constructor Injection (Best Practice)
    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository, SystemLogService systemLogService) {
        this.attendanceRepository = attendanceRepository;
        this.systemLogService = systemLogService;
    }

    public List<Attendance> getAllAttendanceRecords() {
        return attendanceRepository.findAll();
    }

    // --- Βασικές Μέθοδοι Λογικής ---

    public Attendance getTodayOrNull(Employee employee, LocalDate date) {
        return attendanceRepository.findByEmployeeAndDate(employee, date).orElse(null);
    }

    @Transactional
    public void checkIn(Employee employee, LocalDate date, LocalTime time) {
        Optional<Attendance> existing = attendanceRepository.findByEmployeeAndDate(employee, date);

        if (existing.isPresent()) {
            throw new IllegalStateException("Ο υπάλληλος έχει κάνει ήδη Check In σήμερα.");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(date);
        attendance.setCheckInTime(time);

        attendanceRepository.save(attendance);

        // --- AUDIT LOG ---
        systemLogService.log("CHECK_IN",
                String.format("Employee: %s %s checked in at %s",
                        employee.getFirstName(), employee.getLastName(), time));
    }

    @Transactional
    public void checkOut(Employee employee, LocalDate date, LocalTime time) {
        Attendance attendance = attendanceRepository.findByEmployeeAndDate(employee, date)
                .orElseThrow(() -> new IllegalStateException("Δεν βρέθηκε Check In για σήμερα. Κάντε πρώτα Check In."));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("Ο υπάλληλος έχει κάνει ήδη Check Out.");
        }

        attendance.setCheckOutTime(time);
        attendanceRepository.save(attendance);

        // --- AUDIT LOG ---
        systemLogService.log("CHECK_OUT",
                String.format("Employee: %s %s checked out at %s",
                        employee.getFirstName(), employee.getLastName(), time));
    }

    // --- Μέθοδοι Υπολογισμού Μισθοδοσίας ---

    public double calculateTotalHoursWorked(Employee employee, LocalDate payrollDate) {
        YearMonth yearMonth = YearMonth.from(payrollDate);
        List<Attendance> monthlyRecords = attendanceRepository.findByEmployeeAndDateBetween(
                employee, yearMonth.atDay(1), yearMonth.atEndOfMonth());

        long totalMinutes = 0;
        for (Attendance att : monthlyRecords) {
            if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                totalMinutes += Duration.between(att.getCheckInTime(), att.getCheckOutTime()).toMinutes();
            }
        }
        return totalMinutes / 60.0;
    }

    public double calculateSundayHours(Employee employee, LocalDate payrollDate) {
        YearMonth yearMonth = YearMonth.from(payrollDate);
        List<Attendance> monthlyRecords = attendanceRepository.findByEmployeeAndDateBetween(
                employee, yearMonth.atDay(1), yearMonth.atEndOfMonth());

        long sundayMinutes = 0;
        for (Attendance att : monthlyRecords) {
            if (att.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
                if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                    sundayMinutes += Duration.between(att.getCheckInTime(), att.getCheckOutTime()).toMinutes();
                }
            }
        }
        return sundayMinutes / 60.0;
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
}