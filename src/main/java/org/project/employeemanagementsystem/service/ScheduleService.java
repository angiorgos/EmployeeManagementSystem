package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Schedule;
import org.project.employeemanagementsystem.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SystemSettingService settingService;

    // Constructor Injection
    public ScheduleService(ScheduleRepository scheduleRepository, SystemSettingService settingService) {
        this.scheduleRepository = scheduleRepository;
        this.settingService = settingService;
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    /**
     * Κάνει Validation και Αποθήκευση.
     * Επιστρέφει "OK", ή μήνυμα λάθους (ERROR...), ή μήνυμα προειδοποίησης (WARNING...).
     */
    public String validateAndSave(Schedule newSchedule) {

        // --- 1. ΕΛΕΓΧΟΣ OVERLAP (Δεν αλλάζει) ---
        List<Schedule> daysShifts = scheduleRepository.findByEmployeeAndDate(newSchedule.getEmployee(), newSchedule.getDate());
        for (Schedule existing : daysShifts) {
            if (existing.getId().equals(newSchedule.getId())) continue;
            if (newSchedule.getStartTime().isBefore(existing.getEndTime()) &&
                    newSchedule.getEndTime().isAfter(existing.getStartTime())) {
                return "ERROR: Overlapping shift detected!";
            }
        }

        // --- 2. ΥΠΟΛΟΓΙΣΜΟΣ ΕΒΔΟΜΑΔΙΑΙΟΥ ΟΡΙΟΥ ΑΠΟ ΤΑ SETTINGS ---

        // Τραβάμε το μηνιαίο όριο (π.χ. 173.33 ή 176)
        double monthlySetting = settingService.getDouble("payroll.standard_hours", 173.33);

        // ΜΕΤΑΤΡΟΠΗ ΣΕ ΕΒΔΟΜΑΔΙΑΙΟ: (Μήνας * 12) / 52
        double weeklyLimit = (monthlySetting * 12) / 52.0;

        // Στρογγυλοποίηση (προαιρετικά, για να μην βλέπεις 39.9999)
        // weeklyLimit = Math.round(weeklyLimit * 100.0) / 100.0;

        // --- 3. ΥΠΟΛΟΓΙΣΜΟΣ ΤΡΕΧΟΥΣΩΝ ΩΡΩΝ ---
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

        // --- 4. ΣΥΓΚΡΙΣΗ ---
        String result = "OK";

        // Αν ξεπεράσει το υπολογισμένο όριο
        if (totalHours > weeklyLimit) {
            // Μορφοποίηση μηνύματος (π.χ. "Limit is 40.0h")
            result = String.format("WARNING: Weekly limit (%.1fh) exceeded! Total: %.1fh", weeklyLimit, totalHours);
        }

        scheduleRepository.save(newSchedule);
        return result;
    }

    public List<Schedule> getSchedulesForRange(LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByDateBetween(startDate, endDate);
    }

    public void deleteSchedule(Schedule schedule) {
        scheduleRepository.delete(schedule);
    }
}