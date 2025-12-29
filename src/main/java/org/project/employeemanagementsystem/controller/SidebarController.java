package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import org.project.employeemanagementsystem.util.Navigator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class SidebarController implements Initializable { // Πρόσεξε το implements Initializable

    @Autowired
    private Navigator navigator;

    // Inject τα κουμπιά από το FXML
    @FXML private ToggleButton dashboardBtn;
    @FXML private ToggleButton employeesBtn;
    @FXML private ToggleButton departmentsBtn;
    @FXML private ToggleButton usersBtn;
    @FXML private ToggleButton scheduleBtn;
    @FXML private ToggleButton attendanceBtn;
    @FXML private ToggleButton leavesBtn;
    @FXML private ToggleButton leaveTypeBtn;
    @FXML private ToggleButton holidaysBtn;
    @FXML private ToggleButton payrollBtn;
    @FXML private ToggleButton logsBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        highlightCurrentPage();
    }

    private void highlightCurrentPage() {
        String current = Navigator.CURRENT_VIEW;

        if (current == null || current.isEmpty()) return;

        switch (current) {
            case Navigator.DASHBOARD_VIEW:   dashboardBtn.setSelected(true); break;

            case Navigator.EMPLOYEES_VIEW:   employeesBtn.setSelected(true); break;
            case Navigator.DEPARTMENTS_VIEW: departmentsBtn.setSelected(true); break;
            case Navigator.USERS_VIEW:       usersBtn.setSelected(true); break;

            case Navigator.SCHEDULE_VIEW:    scheduleBtn.setSelected(true); break;
            case Navigator.ATTENDANCE_VIEW:  attendanceBtn.setSelected(true); break;
            case Navigator.LEAVES_VIEW:      leavesBtn.setSelected(true); break;
            case Navigator.HOLIDAYS_VIEW:    holidaysBtn.setSelected(true); break;

            case Navigator.PAYROLL_VIEW:     payrollBtn.setSelected(true); break;

            case Navigator.LOGS_VIEW:        logsBtn.setSelected(true); break;

            default:
                if(dashboardBtn.getToggleGroup() != null)
                    dashboardBtn.getToggleGroup().selectToggle(null);
                break;
        }
    }

    //Navigation Methods
    @FXML public void goToDashboard() { navigator.goToDashboard(); }
    @FXML public void goToEmployees() { navigator.goToEmployees(); }
    @FXML public void goToDepartments() { navigator.goToDepartments(); }
    @FXML public void goToUsers() { navigator.goToUsers(); }
    @FXML public void goToSchedule() { navigator.goToSchedule(); }
    @FXML public void goToAttendance() { navigator.goToAttendance(); }
    @FXML public void goToLeaves() { navigator.goToLeaves(); }
    @FXML public void goToLeaveTypes() { navigator.goToLeaveTypes(); }
    @FXML public void goToHolidays() { navigator.goToHolidays(); }
    @FXML public void goToPayroll() { navigator.goToPayroll(); }
    @FXML public void goToLogs() { navigator.goToLogs(); }
    @FXML public void handleLogout() { navigator.logout(); }
}