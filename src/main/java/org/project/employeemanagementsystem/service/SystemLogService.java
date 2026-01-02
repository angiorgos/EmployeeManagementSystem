package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.repository.SystemLogRepository;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SystemLogService {

    @Autowired private SystemLogRepository logRepository;
    @Autowired private UserSession userSession; // <--- Κάνουμε Inject το Session

    public void log(String action) {
        SystemLog newLog = new SystemLog();
        newLog.setAction(action);
        newLog.setTimestamp(java.time.LocalDateTime.now());

        // Αν υπάρχει συνδεδεμένος χρήστης στο Session, τον παίρνουμε
        if (userSession.getCurrentUser() != null) {
            newLog.setUser(userSession.getCurrentUser());
            newLog.setUsername(userSession.getCurrentUser().getUsername());
        } else {
            newLog.setUsername("System");
        }

        logRepository.save(newLog);
    }


    public void log(String action, String manualUsername) {
        SystemLog newLog = new SystemLog();
        newLog.setAction(action);
        newLog.setUsername(manualUsername);
        newLog.setTimestamp(java.time.LocalDateTime.now());
        logRepository.save(newLog);
    }

    public List<SystemLog> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }

    public void clearAllLogs() {
        logRepository.deleteAll();
    }
}