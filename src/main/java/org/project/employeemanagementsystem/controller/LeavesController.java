package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.project.employeemanagementsystem.service.LeaveTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class LeavesController implements Initializable {

    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private EmployeeService employeeService;
    @Autowired private LeaveTypeService leaveTypeService;

    @FXML private TableView<LeaveRequest> requestsTable;
    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private ComboBox<LeaveType> typeCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Γέμισμα Πίνακα Αιτήσεων
        requestsTable.setItems(FXCollections.observableArrayList(leaveRequestService.getAllRequests()));

        // 2. Γέμισμα Dropdowns για νέα αίτηση
        if (employeeCombo != null) employeeCombo.getItems().setAll(employeeService.getActiveEmployees());
        if (typeCombo != null) typeCombo.getItems().setAll(leaveTypeService.getAllLeaveTypes());
    }
}