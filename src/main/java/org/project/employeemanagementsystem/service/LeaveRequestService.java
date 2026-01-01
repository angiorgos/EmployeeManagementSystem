package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserSession userSession;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               HolidayRepository holidayRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               UserSession userSession) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.holidayRepository = holidayRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.userSession = userSession;
    }

    // --- 1. CURRENT USER HELPER ---
    public Employee getLoggedInEmployee() {
        User currentUser = userSession.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("No user logged in! Please login first.");
        }
        if (currentUser.getEmployee() == null) {
            throw new RuntimeException("Logged in user (" + currentUser.getUsername() + ") is not linked to an Employee profile!");
        }
        return currentUser.getEmployee();
    }

    // --- 2. GETTERS ---
    public List<LeaveRequest> getAllRequests() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    // --- 3. SUBMIT NEW REQUEST (Για το Tab 1 - Πάντα PENDING) ---
    @Transactional
    public void submitRequest(LeaveRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        int requestedDays = calculateWorkDays(request.getStartDate(), request.getEndDate());
        if (requestedDays == 0) {
            throw new RuntimeException("You selected only weekends or holidays!");
        }

        int remaining = getRemainingDays(request.getEmployee(), request.getLeaveType());
        if (requestedDays > remaining) {
            throw new RuntimeException("Not enough leave balance! Remaining: " + remaining + ", Requested: " + requestedDays);
        }

        // ΕΔΩ ΕΙΝΑΙ Η ΛΟΓΙΚΗ ΔΗΜΙΟΥΡΓΙΑΣ
        request.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(request);
    }

    // --- 4. UPDATE EXISTING REQUEST (Για το Tab 2 - Admin Actions) ---
    // ΑΥΤΗ Η ΜΕΘΟΔΟΣ ΕΛΕΙΠΕ
    @Transactional
    public void updateRequestStatus(LeaveRequest request) {
        // Σώζουμε το request όπως μας ήρθε (π.χ. APPROVED ή REJECTED)
        // Χωρίς να το γυρίσουμε σε PENDING
        leaveRequestRepository.save(request);
        System.out.println("SERVICE: Request " + request.getId() + " updated to " + request.getStatus());
    }

    // --- 5. CALCULATIONS ---
    public int getRemainingDays(Employee employee, LeaveType leaveType) {
        int maxAllowed = leaveType.getMaxDays();
        List<LeaveRequest> approvedRequests = leaveRequestRepository
                .findByEmployeeAndLeaveTypeAndStatus(employee, leaveType, LeaveStatus.APPROVED);

        int usedDays = 0;
        for (LeaveRequest req : approvedRequests) {
            if (req.getStartDate().getYear() == LocalDate.now().getYear()) {
                usedDays += calculateWorkDays(req.getStartDate(), req.getEndDate());
            }
        }
        return maxAllowed - usedDays;
    }

    private int calculateWorkDays(LocalDate start, LocalDate end) {
        List<LocalDate> holidays = holidayRepository.findAll().stream()
                .map(Holiday::getDate)
                .collect(Collectors.toList());

        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            boolean isWeekend = (current.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    current.getDayOfWeek() == DayOfWeek.SUNDAY);
            boolean isHoliday = holidays.contains(current);

            if (!isWeekend && !isHoliday) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}