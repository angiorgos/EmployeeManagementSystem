package org.project.employeemanagementsystem.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.service.AttendanceService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class DashboardController extends BaseController implements Initializable {


    @Autowired private EmployeeService employeeService;
    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private AttendanceService attendanceService;

    @FXML private Label totalEmployeesLabel;
    @FXML private Label workingTodayLabel;
    @FXML private Label onLeaveLabel;
    @FXML private Label pendingRequestsLabel;

    @FXML private WebView hiresWebView;
    @FXML private WebView deptWebView;
    @FXML private WebView attendanceWebView;
    @FXML private WebView leavesWebView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(this::loadDashboardData);
    }

    private void loadDashboardData() {
        // 1. Φέρνουμε δεδομένα
        List<Employee> allEmployees = employeeService.getAllEmployees();
        List<LeaveRequest> allLeaves = leaveRequestService.getAllRequests();
        List<Attendance> allAttendance = attendanceService.getAllAttendanceRecords();

        // --- KPI CARDS ---
        totalEmployeesLabel.setText(String.valueOf(allEmployees.size()));

        long workingToday = allAttendance.stream()
                .filter(a -> a.getDate().equals(LocalDate.now()))
                .count();
        workingTodayLabel.setText(String.valueOf(workingToday));

        LocalDate today = LocalDate.now();
        long onLeave = allLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED) // Enum check
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .count();
        onLeaveLabel.setText(String.valueOf(onLeave));

        long pending = allLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING)
                .count();
        pendingRequestsLabel.setText(String.valueOf(pending));


        // --- CHARTS ---

        // 1. Departments (Pie)
        Map<String, Long> deptCounts = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        e -> (e.getDepartment() != null) ? e.getDepartment().getName() : "Unknown",
                        Collectors.counting()
                ));
        initChart(deptWebView, "chart_departments.html", mapToLabels(deptCounts), mapToData(deptCounts));


        // 2. Hires (Line Chart - Monthly)
        Map<Month, Long> hiresPerMonth = allEmployees.stream()
                .filter(e -> e.getHireDate() != null && e.getHireDate().getYear() == LocalDate.now().getYear())
                .collect(Collectors.groupingBy(
                        e -> e.getHireDate().getMonth(),
                        Collectors.counting()
                ));

        // Helper για σωστή σειρά μηνών
        String monthsLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";
        String hiresData = getMonthlyDataString(hiresPerMonth);

        initChart(hiresWebView, "chart_hires.html", monthsLabels, hiresData);


        // 3. Leaves (Line Chart - Monthly) -> ΔΙΟΡΘΩΣΗ ΕΔΩ!
        // Υπολογίζουμε άδειες ανά μήνα έναρξης για να έχει νόημα η γραμμή
        Map<Month, Long> leavesPerMonth = allLeaves.stream()
                .filter(l -> l.getStartDate().getYear() == LocalDate.now().getYear())
                .collect(Collectors.groupingBy(
                        l -> l.getStartDate().getMonth(),
                        Collectors.counting()
                ));

        String leavesData = getMonthlyDataString(leavesPerMonth);
        // Χρησιμοποιούμε τα ίδια labels (μήνες)
        initChart(leavesWebView, "chart_leaves.html", monthsLabels, leavesData);


        // 4. Attendance (Bar Chart - Last 5 Days)
        Map<String, Long> attendanceLastDays = allAttendance.stream()
                .filter(a -> a.getDate().isAfter(LocalDate.now().minusDays(6)))
                .collect(Collectors.groupingBy(
                        a -> a.getDate().toString(),
                        Collectors.counting()
                ));
        initChart(attendanceWebView, "chart_attendance.html", mapToLabels(attendanceLastDays), mapToData(attendanceLastDays));
    }

    // Βοηθητική μέθοδος για να φτιάχνουμε το string δεδομένων [0, 2, 5...] για τους 12 μήνες
    private String getMonthlyDataString(Map<Month, Long> dataMap) {
        StringBuilder sb = new StringBuilder("[");
        for (Month m : Month.values()) {
            sb.append(dataMap.getOrDefault(m, 0L)).append(",");
        }
        if (sb.length() > 1) sb.setLength(sb.length() - 1); // remove last comma
        sb.append("]");
        return sb.toString();
    }


    private void initChart(WebView webView, String htmlFile, String labels, String data) {
        WebEngine engine = webView.getEngine();
        try {
            // Φόρτωση του περιεχομένου HTML ως String (η λύση που δώσαμε πριν για να μην σκάει)
            InputStream is = getClass().getResourceAsStream("/charts/" + htmlFile);
            if (is == null) {
                System.err.println("Could not find chart file: " + htmlFile);
                return;
            }
            String htmlContent = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            engine.loadContent(htmlContent);

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    // Inject data
                    String script = "if (typeof updateChart === 'function') { updateChart(" + labels + ", " + data + "); }";
                    engine.executeScript(script);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String mapToLabels(Map<String, Long> map) {
        return map.keySet().stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String mapToData(Map<String, Long> map) {
        return map.values().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}