package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Holiday;
import org.project.employeemanagementsystem.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final SystemLogService systemLogService; // <--- Προσθήκη για Audit Log

    // Constructor Injection
    @Autowired
    public HolidayService(HolidayRepository holidayRepository, SystemLogService systemLogService) {
        this.holidayRepository = holidayRepository;
        this.systemLogService = systemLogService;
    }

    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc();
    }

    @Transactional
    public Holiday saveHoliday(Holiday holiday) {
        boolean isNew = (holiday.getId() == null);

        Holiday savedHoliday = holidayRepository.save(holiday);

        // --- AUDIT LOG ---
        String action = isNew ? "CREATE_HOLIDAY" : "UPDATE_HOLIDAY";
        String details = "Holiday: " + savedHoliday.getName() + " (" + savedHoliday.getDate() + ")";

        systemLogService.log(action, details);

        return savedHoliday;
    }

    @Transactional
    public void deleteHoliday(Long id) {
        // Βρίσκουμε την αργία πριν τη διαγραφή για να καταγράψουμε ποια ήταν
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found with ID: " + id));

        String holidayInfo = holiday.getName() + " (" + holiday.getDate() + ")";

        holidayRepository.delete(holiday);

        // --- AUDIT LOG ---
        systemLogService.log("DELETE_HOLIDAY", "Deleted: " + holidayInfo);
    }
}