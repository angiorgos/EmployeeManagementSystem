package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final SystemLogService systemLogService;

    // Constructor Injection
    @Autowired
    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository, SystemLogService systemLogService) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.systemLogService = systemLogService;
    }

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional
    public LeaveType saveLeaveType(LeaveType leaveType) {
        boolean isNew = (leaveType.getId() == null);

        LeaveType savedType = leaveTypeRepository.save(leaveType);
        String action = isNew ? "CREATE_LEAVE_TYPE" : "UPDATE_LEAVE_TYPE";
        String details = "Type: " + savedType.getName() + " (Max Days: " + savedType.getMaxDays() + ")";

        systemLogService.log(action, details);

        return savedType;
    }

    @Transactional
    public void deleteLeaveType(Long id) {
        // Βρίσκουμε τον τύπο πριν τη διαγραφή για να καταγράψουμε το όνομα
        LeaveType type = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Type not found with ID: " + id));

        String typeName = type.getName();

        leaveTypeRepository.delete(type);

        systemLogService.log("DELETE_LEAVE_TYPE", "Deleted Type: " + typeName);
    }
}