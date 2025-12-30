package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SystemLogService {

    @Autowired
    private SystemLogRepository systemLogRepository;

    public List<SystemLog> getAllLogs() {
        return systemLogRepository.findAllByOrderByTimestampDesc();
    }

    public void saveLog(SystemLog log) {
        systemLogRepository.save(log);
    }

    public void clearAllLogs() {
        systemLogRepository.deleteAll();
    }
}