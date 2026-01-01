package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemSetting;
import org.project.employeemanagementsystem.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    // ==========================================
    // GETTERS (Ανάκτηση Τιμών)
    // ==========================================

    /**
     * Επιστρέφει κείμενο (String).
     * Αν δεν βρεθεί, επιστρέφει το defaultValue που δίνουμε εμείς.
     */
    public String getString(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Επιστρέφει αριθμό (Double).
     * Κάνει αυτόματα τη μετατροπή από String σε Double.
     * Αν αποτύχει η μετατροπή ή δεν βρεθεί το κλειδί, επιστρέφει το defaultValue.
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
     * Επιστρέφει ακέραιο (Integer) - Χρήσιμο π.χ. για μέρες άδειας
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

    // Ενιαία μέθοδος save που δέχεται String
    public void save(String key, String value) {
        repository.save(new SystemSetting(key, value));
    }

    // Overload: Αν της δώσουμε Double, το μετατρέπει μόνη της σε String
    public void save(String key, Double value) {
        save(key, String.valueOf(value));
    }

    // Overload: Αν της δώσουμε Integer
    public void save(String key, Integer value) {
        save(key, String.valueOf(value));
    }
}