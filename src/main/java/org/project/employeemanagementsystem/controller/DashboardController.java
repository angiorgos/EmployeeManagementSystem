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

    @FXML private WebView hiresWebView;
    @FXML private WebView deptWebView;
    @FXML private WebView attendanceWebView;
    @FXML private WebView leavesWebView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(this::loadDashboardData);
    }

    private void loadDashboardData() {
        List<Employee> allEmployees = employeeService.getAllEmployees();
        List<LeaveRequest> allLeaves = leaveRequestService.getAllRequests();
        List<Attendance> allAttendance = attendanceService.getAllAttendanceRecords();


        int totalEmployees = allEmployees.size();
        totalEmployeesLabel.setText(String.valueOf(totalEmployees));

        long workingToday = allAttendance.stream()
                .filter(a -> a.getDate().equals(LocalDate.now()))
                .count();
        workingTodayLabel.setText(String.valueOf(workingToday));

        LocalDate today = LocalDate.now();
        long onLeave = allLeaves.stream()
                .filter(l -> l.getStatus().toString().equalsIgnoreCase("APPROVED"))
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .count();
        onLeaveLabel.setText(String.valueOf(onLeave));



        Map<String, Long> deptCounts = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        e -> (e.getDepartment() != null) ? e.getDepartment().getName() : "Unknown",
                        Collectors.counting()
                ));
        initChart(deptWebView, "chart_departments.html", mapToLabels(deptCounts), mapToData(deptCounts));



        Map<Month, Long> hiresPerMonth = allEmployees.stream()
                .filter(e -> e.getHireDate() != null && e.getHireDate().getYear() == LocalDate.now().getYear())
                .collect(Collectors.groupingBy(
                        e -> e.getHireDate().getMonth(),
                        Collectors.counting()
                ));

        String hiresLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";
        String hiresData = "[" +
                hiresPerMonth.getOrDefault(Month.JANUARY, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.FEBRUARY, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.MARCH, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.APRIL, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.MAY, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.JUNE, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.JULY, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.AUGUST, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.SEPTEMBER, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.OCTOBER, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.NOVEMBER, 0L) + "," +
                hiresPerMonth.getOrDefault(Month.DECEMBER, 0L) + "]";
        initChart(hiresWebView, "chart_hires.html", hiresLabels, hiresData);



        Map<String, Long> leavesByType = allLeaves.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getLeaveType() != null) ? l.getLeaveType().getName() : "Other",
                        Collectors.counting()
                ));
        initChart(leavesWebView, "chart_leaves.html", mapToLabels(leavesByType), mapToData(leavesByType));



        Map<String, Long> attendanceLastDays = allAttendance.stream()
                .filter(a -> a.getDate().isAfter(LocalDate.now().minusDays(6))) // Τελευταίες 5-6 μέρες
                .collect(Collectors.groupingBy(
                        a -> a.getDate().toString(),
                        Collectors.counting()
                ));
        initChart(attendanceWebView, "chart_attendance.html", mapToLabels(attendanceLastDays), mapToData(attendanceLastDays));
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