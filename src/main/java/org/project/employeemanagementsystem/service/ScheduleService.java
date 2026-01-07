package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Schedule;
import org.project.employeemanagementsystem.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SystemSettingService settingService;
    private final SystemLogService systemLogService;

    // Constructor Injection
    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository,
                           SystemSettingService settingService,
                           SystemLogService systemLogService) {
        this.scheduleRepository = scheduleRepository;
        this.settingService = settingService;
        this.systemLogService = systemLogService;
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> getSchedulesForRange(LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByDateBetween(startDate, endDate);
    }


    @Transactional
    public String validateAndSave(Schedule newSchedule) {
        boolean isNew = (newSchedule.getId() == null);


        List<Schedule> daysShifts = scheduleRepository.findByEmployeeAndDate(newSchedule.getEmployee(), newSchedule.getDate());
        for (Schedule existing : daysShifts) {
            if (existing.getId().equals(newSchedule.getId())) continue;
            if (newSchedule.getStartTime().isBefore(existing.getEndTime()) &&
                    newSchedule.getEndTime().isAfter(existing.getStartTime())) {
                return "ERROR: Overlapping shift detected!";
            }
        }

        //ΥΠΟΛΟΓΙΣΜΟΣ ΕΒΔΟΜΑΔΙΑΙΟΥ ΟΡΙΟΥ
        double monthlySetting = settingService.getDouble("payroll.standard_hours", 173.33);
        double weeklyLimit = (monthlySetting * 12) / 52.0;

        //ΥΠΟΛΟΓΙΣΜΟΣ ΤΡΕΧΟΥΣΩΝ ΩΡΩΝ
        LocalDate date = newSchedule.getDate();
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Schedule> weeklyShifts = scheduleRepository.findByEmployeeAndDateBetween(
                newSchedule.getEmployee(), startOfWeek, endOfWeek
        );

        long totalMinutes = 0;
        for (Schedule s : weeklyShifts) {
            if (s.getId().equals(newSchedule.getId())) continue;
            totalMinutes += Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
        }

        // Προσθήκη της νέας βάρδιας
        totalMinutes += Duration.between(newSchedule.getStartTime(), newSchedule.getEndTime()).toMinutes();
        double totalHours = totalMinutes / 60.0;


        String result = "OK";

        if (totalHours > weeklyLimit) {
            result = String.format("WARNING: Weekly limit (%.1fh) exceeded! Total: %.1fh", weeklyLimit, totalHours);
        }

        scheduleRepository.save(newSchedule);

        String action = isNew ? "CREATE_SCHEDULE" : "UPDATE_SCHEDULE";
        String details = String.format("Employee: %s %s, Date: %s, Time: %s-%s",
                newSchedule.getEmployee().getFirstName(), newSchedule.getEmployee().getLastName(),
                newSchedule.getDate(), newSchedule.getStartTime(), newSchedule.getEndTime());

        if (result.startsWith("WARNING")) {
            details += " [OVERTIME WARNING]";
        }

        systemLogService.log(action, details);

        return result;
    }

    @Transactional
    public void deleteSchedule(Schedule schedule) {
        String details = String.format("Deleted Shift: %s %s on %s",
                schedule.getEmployee().getFirstName(), schedule.getEmployee().getLastName(),
                schedule.getDate());

        scheduleRepository.delete(schedule);

        systemLogService.log("DELETE_SCHEDULE", details);
    }
}