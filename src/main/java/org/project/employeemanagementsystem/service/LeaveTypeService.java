package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LeaveTypeService {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional
    public LeaveType saveLeaveType(LeaveType leaveType) {
        return leaveTypeRepository.save(leaveType);
    }

    @Transactional
    public void deleteLeaveType(Long id) {
        leaveTypeRepository.deleteById(id);
    }
}