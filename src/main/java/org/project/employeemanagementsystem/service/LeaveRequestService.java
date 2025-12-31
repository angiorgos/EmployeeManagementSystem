package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository; // Το χρειαζόμαστε για το Login
    private final LeaveTypeRepository leaveTypeRepository; // Για το ComboBox

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               HolidayRepository holidayRepository,
                               UserRepository userRepository,
                               LeaveTypeRepository leaveTypeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.holidayRepository = holidayRepository;
        this.userRepository = userRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    // --- 1. ΕΥΡΕΣΗ ΣΥΝΔΕΔΕΜΕΝΟΥ ΥΠΑΛΛΗΛΟΥ (NEW) ---
    public Employee getLoggedInEmployee() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            if (user.getEmployee() == null) {
                throw new RuntimeException("Logged in user is not linked to an Employee profile!");
            }
            return user.getEmployee();
        }
        throw new RuntimeException("No user logged in");
    }

    // --- 2. ΒΑΣΙΚΕΣ ΜΕΘΟΔΟΙ (CRUD) ---
    public List<LeaveRequest> getAllRequests() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    public List<LeaveRequest> getRequestsByEmployee(Employee employee) {
        return leaveRequestRepository.findByEmployee(employee);
    }

    // --- 3. ΥΠΟΒΟΛΗ ΑΙΤΗΣΗΣ ΜΕ ΕΛΕΓΧΟΥΣ (UPDATED) ---
    @Transactional
    public void submitRequest(LeaveRequest request) {
        // Έλεγχος ημερομηνιών
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        // Υπολογισμός ημερών που ζητάει
        int requestedDays = calculateWorkDays(request.getStartDate(), request.getEndDate());
        if (requestedDays == 0) {
            throw new RuntimeException("You selected only weekends or holidays!");
        }

        // Υπολογισμός υπολοίπου
        int remaining = getRemainingDays(request.getEmployee(), request.getLeaveType());
        if (requestedDays > remaining) {
            throw new RuntimeException("Not enough leave balance! Remaining: " + remaining + ", Requested: " + requestedDays);
        }

        // Ορισμός status και αποθήκευση
        request.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(request);
    }

    // --- 4. ΥΠΟΛΟΓΙΣΜΟΣ ΥΠΟΛΟΙΠΟΥ (YOUR LOGIC REFINED) ---
    public int getRemainingDays(Employee employee, LeaveType leaveType) {
        int maxAllowed = leaveType.getMaxDays();

        // Προσοχή: Χρησιμοποιούμε το Enum LeaveStatus.APPROVED
        List<LeaveRequest> approvedRequests = leaveRequestRepository
                .findByEmployeeAndLeaveTypeAndStatus(employee, leaveType, LeaveStatus.APPROVED);

        int usedDays = 0;
        for (LeaveRequest req : approvedRequests) {
            // Μετράμε μόνο για το τρέχον έτος
            if (req.getStartDate().getYear() == LocalDate.now().getYear()) {
                usedDays += calculateWorkDays(req.getStartDate(), req.getEndDate());
            }
        }
        return maxAllowed - usedDays;
    }

    // Βοηθητική μέθοδος υπολογισμού εργάσιμων (με χρήση Holiday Repo)
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

    public void deleteRequest(LeaveRequest request) {
        leaveRequestRepository.delete(request);
    }
}