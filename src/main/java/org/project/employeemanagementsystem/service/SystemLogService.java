package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.SystemLogRepository;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemLogService {

    private final SystemLogRepository logRepository;
    private final UserSession userSession;

    @Autowired
    public SystemLogService(SystemLogRepository logRepository, UserSession userSession) {
        this.logRepository = logRepository;
        this.userSession = userSession;
    }

    /**
     * 1. Απλή καταγραφή (Action μόνο)
     */
    @Transactional
    public void log(String action) {
        createAndSaveLog(action, null);
    }

    /**
     * 2. Καταγραφή με Λεπτομέρειες (Action + Details String)
     * <-- ΑΥΤΗ ΕΛΕΙΠΕ ΚΑΙ ΧΤΥΠΟΥΣΕ ΤΟ ERROR
     */
    @Transactional
    public void log(String action, String details) {
        // Συνδυάζουμε το action με τις λεπτομέρειες
        String combinedAction = action + ": " + details;

        // Ασφάλεια: Κόβουμε το string αν είναι πολύ μεγάλο για τη βάση (255 chars)
        if (combinedAction.length() > 255) {
            combinedAction = combinedAction.substring(0, 252) + "...";
        }

        createAndSaveLog(combinedAction, null);
    }

    /**
     * 3. Καταγραφή με συγκεκριμένο χρήστη (π.χ. για Admin actions ή Seeder)
     */
    @Transactional
    public void log(String action, User specificUser) {
        createAndSaveLog(action, specificUser);
    }

    // --- Εσωτερική βοηθητική μέθοδος για να μην γράφουμε τον ίδιο κώδικα 3 φορές ---
    private void createAndSaveLog(String actionText, User specificUser) {
        SystemLog newLog = new SystemLog();
        newLog.setAction(actionText);
        newLog.setTimestamp(LocalDateTime.now());

        try {
            // Αν μας έδωσαν συγκεκριμένο χρήστη, βάζουμε αυτόν
            if (specificUser != null) {
                newLog.setUser(specificUser);
                newLog.setUsername(specificUser.getUsername());
            }
            // Αλλιώς ψάχνουμε το Session
            else if (userSession != null && userSession.getCurrentUser() != null) {
                User currentUser = userSession.getCurrentUser();
                newLog.setUser(currentUser);
                newLog.setUsername(currentUser.getUsername());
            }
            // Αν δεν βρούμε τίποτα (π.χ. Seeder)
            else {
                newLog.setUsername("System");
            }
        } catch (Exception e) {
            newLog.setUsername("Unknown");
        }

        logRepository.save(newLog);
    }

    public List<SystemLog> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public void clearAllLogs() {
        User currentUser = (userSession != null) ? userSession.getCurrentUser() : null;
        logRepository.deleteAll();

        // Καταγραφή της διαγραφής
        log("SYSTEM_LOGS_CLEARED", currentUser);
    }
}