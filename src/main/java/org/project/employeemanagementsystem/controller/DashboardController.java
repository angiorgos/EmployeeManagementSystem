package org.project.employeemanagementsystem.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ProgressIndicator;
import javafx.animation.ScaleTransition;
import javafx.animation.Animation;
import org.kordamp.ikonli.javafx.FontIcon;
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
    @FXML private VBox loadingOverlay;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(this::loadDashboardData);
    }

    private void loadDashboardData() {
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                // ΕΔΩ ΤΡΕΧΟΥΝ ΤΑ ΒΑΡΙΑ SQL Queries (Δεν παγώνει το UI)
                List<Employee> employees = employeeService.getAllEmployees();
                List<LeaveRequest> leaves = leaveRequestService.getAllRequests();
                List<Attendance> attendance = attendanceService.getAllAttendanceRecords();

                return new DashboardData(employees, leaves, attendance);
            }
        };

        task.setOnSucceeded(event -> {
            DashboardData data = task.getValue();

            // --- ΕΝΗΜΕΡΩΣΗ KPI LABELS ---
            totalEmployeesLabel.setText(String.valueOf(data.employees.size()));

            long workingToday = data.attendance.stream()
                    .filter(a -> a.getDate().equals(LocalDate.now()))
                    .count();
            workingTodayLabel.setText(String.valueOf(workingToday));

            LocalDate today = LocalDate.now();
            long onLeave = data.leaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                    .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                    .count();
            onLeaveLabel.setText(String.valueOf(onLeave));

            long pending = data.leaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.PENDING)
                    .count();
            pendingRequestsLabel.setText(String.valueOf(pending));

            //Departments
            Map<String, Long> deptCounts = data.employees.stream()
                    .collect(Collectors.groupingBy(
                            e -> (e.getDepartment() != null) ? e.getDepartment().getName() : "Unknown",
                            Collectors.counting()
                    ));
            initChart(deptWebView, "chart_departments.html", mapToLabels(deptCounts), mapToData(deptCounts));

            //Hires
            Map<Month, Long> hiresPerMonth = data.employees.stream()
                    .filter(e -> e.getHireDate() != null && e.getHireDate().getYear() == LocalDate.now().getYear())
                    .collect(Collectors.groupingBy(e -> e.getHireDate().getMonth(), Collectors.counting()));

            String monthsLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";
            initChart(hiresWebView, "chart_hires.html", monthsLabels, getMonthlyDataString(hiresPerMonth));

            //Leaves
            Map<Month, Long> leavesPerMonth = data.leaves.stream()
                    .filter(l -> l.getStartDate().getYear() == LocalDate.now().getYear())
                    .collect(Collectors.groupingBy(l -> l.getStartDate().getMonth(), Collectors.counting()));
            initChart(leavesWebView, "chart_leaves.html", monthsLabels, getMonthlyDataString(leavesPerMonth));

            //Attendance
            Map<String, Long> attendanceLastDays = data.attendance.stream()
                    .filter(a -> a.getDate().isAfter(LocalDate.now().minusDays(6)))
                    .collect(Collectors.groupingBy(a -> a.getDate().toString(), Collectors.counting()));
            initChart(attendanceWebView, "chart_attendance.html", mapToLabels(attendanceLastDays), mapToData(attendanceLastDays));

            //ΑΦΑΙΡΕΣΗ LOADING SCREEN
            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
                fadeOut.play();
            });
            delay.play();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            loadingOverlay.setVisible(false);
        });

        new Thread(task).start();
    }

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
            // 1. Φόρτωση του HTML
            String url = getClass().getResource("/charts/" + htmlFile).toExternalForm();
            engine.load(url);

            // 2. Listener για το πότε τελείωσε η φόρτωση
            engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {

                    // 3. ✨ ΤΟ ΚΟΛΠΟ: Καθυστέρηση 1 δευτερολέπτου ✨
                    // Δίνουμε χρόνο στο JavaFX να υπολογίσει το πλάτος του WebView πριν ζωγραφίσει το Chart
                    PauseTransition delay = new PauseTransition(Duration.seconds(1));
                    delay.setOnFinished(event -> {
                        try {
                            // Τώρα τρέχουμε το script με ασφάλεια
                            engine.executeScript("if (window.updateChart) { window.updateChart(" + labels + ", " + data + "); }");
                        } catch (Exception e) {
                            System.err.println("Error executing script for " + htmlFile + ": " + e.getMessage());
                        }
                    });
                    delay.play();
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

    private static class DashboardData {
        List<Employee> employees;
        List<LeaveRequest> leaves;
        List<Attendance> attendance;

        public DashboardData(List<Employee> employees, List<LeaveRequest> leaves, List<Attendance> attendance) {
            this.employees = employees;
            this.leaves = leaves;
            this.attendance = attendance;
        }
    }
}

