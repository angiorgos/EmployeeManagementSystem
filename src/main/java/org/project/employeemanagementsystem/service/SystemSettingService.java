package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemSetting;
import org.project.employeemanagementsystem.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final SystemLogService systemLogService; // <--- Προσθήκη Audit

    // Constructor Injection
    @Autowired
    public SystemSettingService(SystemSettingRepository repository, SystemLogService systemLogService) {
        this.repository = repository;
        this.systemLogService = systemLogService;
    }

    // ==========================================
    // GETTERS (Ανάκτηση Τιμών)
    // ==========================================

    /**
     * Επιστρέφει κείμενο (String).
     */
    public String getString(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Επιστρέφει αριθμό (Double).
     */
    public Double getDouble(String key, Double defaultValue) {
        return repository.findById(key)
                .map(setting -> {
                    try {
                        return Double.parseDouble(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Επιστρέφει ακέραιο (Integer).
     */
    public Integer getInt(String key, Integer defaultValue) {
        return repository.findById(key)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    // ==========================================
    // SETTERS (Αποθήκευση)
    // ==========================================

    /**
     * Ενιαία μέθοδος save που δέχεται String.
     * Εδώ γίνεται και το Logging.
     */
    @Transactional
    public void save(String key, String value) {
        // Αποθήκευση
        repository.save(new SystemSetting(key, value));

        // --- AUDIT LOG ---
        // Καταγράφουμε ότι άλλαξε η συγκεκριμένη ρύθμιση
        systemLogService.log("UPDATE_SETTING", String.format("Key: %s | New Value: %s", key, value));
    }

    // Overload: Αν της δώσουμε Double
    public void save(String key, Double value) {
        save(key, String.valueOf(value));
    }

    // Overload: Αν της δώσουμε Integer
    public void save(String key, Integer value) {
        save(key, String.valueOf(value));
    }
}