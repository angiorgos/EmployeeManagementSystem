package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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

    // Χρήσιμο για να γεμίζεις πίνακες τύπου "Who is here today"
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public void saveAttendance(Attendance attendance) {
        attendanceRepository.save(attendance);
    }

    public void deleteAttendance(Attendance attendance) {
        attendanceRepository.delete(attendance);
    }
}