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

        // Αν είναι SystemLog, σταματάμε για να μην έχουμε infinite loop
        if (entity instanceof org.project.employeemanagementsystem.model.SystemLog) return;

        // ΕΙΔΙΚΟΣ ΕΛΕΓΧΟΣ ΓΙΑ USER
        if (entity instanceof User user) {
            // Αν είναι Update (δηλαδή Login), ΜΗΝ το καταγράφεις αυτόματα
            if (operation.equals("Updated")) return;

            // Αν είναι Created ή Deleted, κατέγραψέ το κανονικά
            logService.log(operation + " User Account: " + user.getUsername());
            return;
        }

        // ΓΙΑ ΟΛΑ ΤΑ ΑΛΛΑ ENTITIES (Employee, Department κλπ)
        String description = operation + " " + entity.getClass().getSimpleName();
        if (entity instanceof Employee emp) {
            description += ": " + emp.getFirstName() + " " + emp.getLastName();
        }
        logService.log(description);
    }
}