package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Schedule;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class ScheduleController implements Initializable {

    @Autowired private ScheduleService scheduleService;
    @Autowired private EmployeeService employeeService; // Για να διαλέγουμε υπάλληλο

    @FXML private TableView<Schedule> scheduleTable;
    @FXML private ComboBox<Employee> employeeComboBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Φόρτωση Προγράμματος
        scheduleTable.setItems(FXCollections.observableArrayList(scheduleService.getAllSchedules()));

        // Φόρτωση ΜΟΝΟ των ενεργών υπαλλήλων στο dropdown
        if (employeeComboBox != null) {
            employeeComboBox.getItems().setAll(employeeService.getActiveEmployees());
        }
    }
}