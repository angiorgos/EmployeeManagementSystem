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

    public int getRemainingDays(Employee employee, LeaveType leaveType) {
        int maxAllowed = leaveType.getMaxDays();

        // 1. Παίρνουμε όλες τις APPROVED αιτήσεις
        List<LeaveRequest> approvedRequests = leaveRequestRepository
                .findByEmployeeAndLeaveTypeAndStatus_Name(employee, leaveType, "APPROVED");

        // 2. Παίρνουμε όλες τις αργίες από τη βάση για να τις εξαιρέσουμε
        List<LocalDate> holidays = holidayRepository.findAll().stream()
                .map(Holiday::getDate)
                .collect(Collectors.toList());

        // 3. Υπολογίζουμε τις πραγματικές μέρες εργασίας που καταναλώθηκαν
        int usedDays = 0;
        for (LeaveRequest request : approvedRequests) {
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
            // Έλεγχος αν ΔΕΝ είναι Σαββατοκύριακο (6=Σάββατο, 7=Κυριακή)
            boolean isWeekend = (current.getDayOfWeek().getValue() == 6 || current.getDayOfWeek().getValue() == 7);
            // Έλεγχος αν ΔΕΝ είναι καταχωρημένη αργία
            boolean isHoliday = holidays.contains(current);

            if (!isWeekend && !isHoliday) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}