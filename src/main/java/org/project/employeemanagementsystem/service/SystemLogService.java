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


    @Transactional
    public void log(String action) {
        createAndSaveLog(action, null);
    }

    @Transactional
    public void log(String action, String details) {
        String combinedAction = action + ": " + details;
        if (combinedAction.length() > 255) {
            combinedAction = combinedAction.substring(0, 252) + "...";
        }

        createAndSaveLog(combinedAction, null);
    }

    @Transactional
    public void log(String action, User specificUser) {
        createAndSaveLog(action, specificUser);
    }
    private void createAndSaveLog(String actionText, User specificUser) {
        SystemLog newLog = new SystemLog();
        newLog.setAction(actionText);
        newLog.setTimestamp(LocalDateTime.now());

        try {
            if (specificUser != null) {
                newLog.setUser(specificUser);
                newLog.setUsername(specificUser.getUsername());
            }
            else if (userSession != null && userSession.getCurrentUser() != null) {
                User currentUser = userSession.getCurrentUser();
                newLog.setUser(currentUser);
                newLog.setUsername(currentUser.getUsername());
            }
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
        log("SYSTEM_LOGS_CLEARED", currentUser);
    }
}