package org.project.employeemanagementsystem.service;

import jakarta.transaction.Transactional;
import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.repository.SystemLogRepository;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemLogService {

    @Autowired private SystemLogRepository logRepository;
    @Autowired private UserSession userSession; // <--- Για να βρίσκει αυτόματα τον User

    @Transactional
    public void log(String action) {
        SystemLog newLog = new SystemLog();
        newLog.setAction(action);
        newLog.setTimestamp(LocalDateTime.now());

        if (userSession != null && userSession.getCurrentUser() != null) {
            newLog.setUser(userSession.getCurrentUser());
            newLog.setUsername(userSession.getCurrentUser().getUsername());
        } else {
            newLog.setUsername("System");
        }

        logRepository.save(newLog);
        logRepository.flush();
    }

    public List<SystemLog> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }
    
    @Transactional
    public void clearAllLogs() {
        logRepository.deleteAll();
    }
}