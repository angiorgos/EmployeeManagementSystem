package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.repository.LeaveStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LeaveStatusService {

    @Autowired
    private LeaveStatusRepository leaveStatusRepository;

    public List<LeaveStatus> getAllStatuses() {
        return leaveStatusRepository.findAll();
    }

    // Helper μέθοδος για να βρίσκουμε εύκολα το status που θέλουμε (π.χ. "PENDING")
    public LeaveStatus getByName(String name) {
        return leaveStatusRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Leave Status '" + name + "' not found within Database."));
    }
}