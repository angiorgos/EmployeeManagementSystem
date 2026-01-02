package org.project.employeemanagementsystem.util;

import jakarta.persistence.*;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AuditListener {

    private static SystemLogService logService;

    @Autowired
    public void setLogService(@Lazy SystemLogService logService) {
        AuditListener.logService = logService;
    }

    @PostPersist
    public void afterSave(Object entity) {
        logAction("Created", entity);
    }

    @PostUpdate
    public void afterUpdate(Object entity) {
        logAction("Updated", entity);
    }

    @PostRemove
    public void afterDelete(Object entity) {
        logAction("Deleted", entity);
    }

    private void logAction(String operation, Object entity) {
        if (logService == null) return;

        String description = operation + " " + entity.getClass().getSimpleName();

        if (entity instanceof Employee emp) {
            description += ": " + emp.getFirstName() + " " + emp.getLastName();
        } else if (entity instanceof User user) {
            description += ": " + user.getUsername();
        }


        logService.log(description);
    }
}