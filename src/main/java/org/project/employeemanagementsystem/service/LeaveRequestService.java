package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.HolidayRepository;
import org.project.employeemanagementsystem.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Calculates the remaining days for a specific leave type.
     */
    public int getRemainingDays(Employee employee, LeaveType leaveType) {
        if (leaveType.getMaxDays() == null) return 999; // Unlimited (e.g. Sick Leave)

        Integer used = leaveRequestRepository.countUsedDays(
                employee,
                leaveType,
                LocalDate.now().getYear()
        );

        return leaveType.getMaxDays() - (used != null ? used : 0);
    }

    /**
     * Core method to submit a new leave request with validation.
     */
    @Transactional
    public LeaveRequest submitRequest(LeaveRequest request) {
        // 1. Calculate actual duration (excluding weekends and official holidays)
        int duration = calculateWorkDays(request.getStartDate(), request.getEndDate());

        // 2. Check if employee has enough remaining days
        int remaining = getRemainingDays(request.getEmployee(), request.getLeaveType());
        if (duration > remaining) {
            throw new RuntimeException("Insufficient leave balance. Requested: " + duration + ", Available: " + remaining);
        }

        // 3. Save the request
        return leaveRequestRepository.save(request);
    }

    /**
     * Helper method to count working days only.
     */
    private int calculateWorkDays(LocalDate start, LocalDate end) {
        List<LocalDate> officialHolidays = holidayRepository.findAll().stream()
                .map(Holiday::getDate)
                .collect(Collectors.toList());

        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            // Check if it's NOT a weekend AND NOT an official holiday
            boolean isWeekend = (current.getDayOfWeek().getValue() == 6 || current.getDayOfWeek().getValue() == 7);
            boolean isHoliday = officialHolidays.contains(current);

            if (!isWeekend && !isHoliday) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}