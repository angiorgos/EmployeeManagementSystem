package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.util.Navigator;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class SidebarController implements Initializable {

    @Autowired
    private Navigator navigator;

    @Autowired
    private UserSession userSession;

    // Buttons
    @FXML private ToggleButton dashboardBtn;

    // WORKFORCE
    @FXML private ToggleButton employeesBtn;
    @FXML private ToggleButton departmentsBtn;
    @FXML private ToggleButton usersBtn;

    // TIME & ATTENDANCE
    @FXML private ToggleButton scheduleBtn;
    @FXML private ToggleButton attendanceBtn;
    @FXML private ToggleButton leavesBtn;
    @FXML private ToggleButton leaveTypeBtn;
    @FXML private ToggleButton holidaysBtn;

    // FINANCIALS
    @FXML private ToggleButton payrollBtn;

    // SYSTEM
    @FXML private ToggleButton logsBtn;

    // Section headers
    @FXML private Label workforceHeader;
    @FXML private Label financialsHeader;
    @FXML private Label systemHeader; // always visible

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyRoleVisibility();
        highlightCurrentPage();
    }

    private void applyRoleVisibility() {
        User user = userSession.getCurrentUser();
        String role = user != null && user.getRole() != null
                ? user.getRole().getName()
                : "";

        boolean isAdmin = "Admin".equalsIgnoreCase(role);
        boolean isHR = "HR".equalsIgnoreCase(role);
        boolean isAccountant = "Accountant".equalsIgnoreCase(role);

        //WORKFORCE
        boolean showWorkforce = isAdmin || isHR;

        workforceHeader.setVisible(showWorkforce);
        workforceHeader.setManaged(showWorkforce);

        employeesBtn.setVisible(showWorkforce);
        employeesBtn.setManaged(showWorkforce);

        departmentsBtn.setVisible(showWorkforce);
        departmentsBtn.setManaged(showWorkforce);

        usersBtn.setVisible(isAdmin);
        usersBtn.setManaged(isAdmin);

        //TIME & ATTENDANCE
        attendanceBtn.setVisible(isAdmin || isHR);
        attendanceBtn.setManaged(isAdmin || isHR);

        // Other buttons in this section always visible
        scheduleBtn.setVisible(true);
        scheduleBtn.setManaged(true);

        leavesBtn.setVisible(true);
        leavesBtn.setManaged(true);

        leaveTypeBtn.setVisible(true);
        leaveTypeBtn.setManaged(true);

        holidaysBtn.setVisible(true);
        holidaysBtn.setManaged(true);

        //FINANCIALS
        boolean showFinancials = isAdmin || isAccountant;

        financialsHeader.setVisible(showFinancials);
        financialsHeader.setManaged(showFinancials);

        payrollBtn.setVisible(showFinancials);
        payrollBtn.setManaged(showFinancials);

        // SYSTEM
        // SYSTEM header always visible
        systemHeader.setVisible(true);
        systemHeader.setManaged(true);

        // Logs button only visible to Admin
        logsBtn.setVisible(isAdmin);
        logsBtn.setManaged(isAdmin);
    }

    private void highlightCurrentPage() {
        String current = Navigator.CURRENT_VIEW;
        if (current == null || current.isEmpty()) return;

        switch (current) {
            case Navigator.DASHBOARD_VIEW -> dashboardBtn.setSelected(true);

            case Navigator.EMPLOYEES_VIEW -> employeesBtn.setSelected(true);
            case Navigator.DEPARTMENTS_VIEW -> departmentsBtn.setSelected(true);
            case Navigator.USERS_VIEW -> usersBtn.setSelected(true);

            case Navigator.SCHEDULE_VIEW -> scheduleBtn.setSelected(true);
            case Navigator.ATTENDANCE_VIEW -> attendanceBtn.setSelected(true);
            case Navigator.LEAVES_VIEW -> leavesBtn.setSelected(true);
            case Navigator.LEAVESTYPES_VIEW -> leaveTypeBtn.setSelected(true);
            case Navigator.HOLIDAYS_VIEW -> holidaysBtn.setSelected(true);

            case Navigator.PAYROLL_VIEW -> payrollBtn.setSelected(true);
            case Navigator.LOGS_VIEW -> logsBtn.setSelected(true);

            default -> {
                if (dashboardBtn.getToggleGroup() != null) {
                    dashboardBtn.getToggleGroup().selectToggle(null);
                }
            }
        }
    }

    // Navigation
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
