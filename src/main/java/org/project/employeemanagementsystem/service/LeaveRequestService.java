package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.HolidayRepository;
import org.project.employeemanagementsystem.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private HolidayRepository holidayRepository;


    public List<LeaveRequest> getAllRequests() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveRequest> getRequestsByEmployee(Employee employee) {
        return leaveRequestRepository.findByEmployee(employee);
    }

    public void saveRequest(LeaveRequest request) {
        // Έλεγχος: Η ημερομηνία λήξης να μην είναι πριν την έναρξη
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }
        leaveRequestRepository.save(request);
    }

    public void deleteRequest(LeaveRequest request) {
        leaveRequestRepository.delete(request);
    }

    //Υπολογισμός Υπολοίπου

    public int getRemainingDays(Employee employee, LeaveType leaveType) {
        int maxAllowed = leaveType.getMaxDays();

        // Βρίσκουμε μόνο τις ΕΓΚΕΚΡΙΜΕΝΕΣ (APPROVED) άδειες
        List<LeaveRequest> approvedRequests = leaveRequestRepository
                .findByEmployeeAndLeaveTypeAndStatus_Name(employee, leaveType, "APPROVED");

        // Φέρνουμε τις αργίες
        List<LocalDate> holidays = holidayRepository.findAll().stream()
                .map(Holiday::getDate)
                .collect(Collectors.toList());

        // Υπολογίζουμε πόσες εργάσιμες "έκαψε"
        int usedDays = 0;
        for (LeaveRequest request : approvedRequests) {
            // Μετράμε μόνο για το τρέχον έτος
            if (request.getStartDate().getYear() == LocalDate.now().getYear()) {
                usedDays += calculateWorkDays(request.getStartDate(), request.getEndDate(), holidays);
            }
        }

        return maxAllowed - usedDays;
    }

    private int calculateWorkDays(LocalDate start, LocalDate end, List<LocalDate> holidays) {
        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            // Εξαιρούμε Σαββατοκύριακα (6=Sat, 7=Sun)
            boolean isWeekend = (current.getDayOfWeek().getValue() == 6 || current.getDayOfWeek().getValue() == 7);
            // Εξαιρούμε Αργίες
            boolean isHoliday = holidays.contains(current);

            if (!isWeekend && !isHoliday) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}