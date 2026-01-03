package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.service.AttendanceService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class DashboardController implements Initializable {

    @Autowired private EmployeeService employeeService;
    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private AttendanceService attendanceService;

    // KPI Labels
    @FXML private Label totalEmployeesLabel;
    @FXML private Label workingTodayLabel;
    @FXML private Label onLeaveLabel;
    @FXML private Label pendingRequestsLabel;

    // WebViews
    @FXML private WebView hiresWebView;
    @FXML private WebView deptWebView;
    @FXML private WebView attendanceWebView;
    @FXML private WebView leavesWebView;

    // ComboBoxes (New)
    @FXML private ComboBox<Integer> hiresYearCombo;
    @FXML private ComboBox<Integer> leavesYearCombo;
    @FXML private ComboBox<Integer> attendanceYearCombo;

    @FXML private VBox loadingOverlay;

    // --- CACHED DATA (Για να μην χτυπάμε τη βάση σε κάθε αλλαγή έτους) ---
    private List<Employee> cachedEmployees = new ArrayList<>();
    private List<LeaveRequest> cachedLeaves = new ArrayList<>();
    private List<Attendance> cachedAttendance = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupYearComboBoxes();
        Platform.runLater(this::loadDashboardData);
    }

    private void setupYearComboBoxes() {
        int currentYear = LocalDate.now().getYear();
        // Γέμισμα με έτη (π.χ. από 2020 έως του χρόνου)
        List<Integer> years = IntStream.rangeClosed(currentYear - 5, currentYear + 1)
                .boxed().collect(Collectors.toList());

        // Ρύθμιση Hires Combo
        hiresYearCombo.getItems().addAll(years);
        hiresYearCombo.setValue(currentYear);
        hiresYearCombo.setOnAction(e -> updateHiresChart(hiresYearCombo.getValue()));

        // Ρύθμιση Leaves Combo
        leavesYearCombo.getItems().addAll(years);
        leavesYearCombo.setValue(currentYear);
        leavesYearCombo.setOnAction(e -> updateLeavesChart(leavesYearCombo.getValue()));

        // Ρύθμιση Attendance Combo
        attendanceYearCombo.getItems().addAll(years);
        attendanceYearCombo.setValue(currentYear);
        attendanceYearCombo.setOnAction(e -> updateAttendanceChart(attendanceYearCombo.getValue()));
    }

    public void refresh() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Φόρτωση όλων των δεδομένων στις cached λίστες
                cachedEmployees = employeeService.getAllEmployees();
                cachedLeaves = leaveRequestService.getAllRequests();
                cachedAttendance = attendanceService.getAllAttendanceRecords();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            updateKPILabels();
            updateDepartmentsChart(); // Αυτό είναι σταθερό (active employees)

            // Ενημέρωση των γραφημάτων με βάση τα επιλεγμένα έτη στα ComboBoxes
            updateHiresChart(hiresYearCombo.getValue());
            updateLeavesChart(leavesYearCombo.getValue());
            updateAttendanceChart(attendanceYearCombo.getValue());

            fadeOutLoading();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
        });

        new Thread(task).start();
    }

    // --- UPDATE METHODS ---

    private void updateKPILabels() {
        long activeCount = cachedEmployees.stream().filter(Employee::isActive).count();
        totalEmployeesLabel.setText(String.valueOf(activeCount));

        long workingToday = cachedAttendance.stream()
                .filter(a -> a.getDate().equals(LocalDate.now()))
                .count();
        workingTodayLabel.setText(String.valueOf(workingToday));

        LocalDate today = LocalDate.now();
        long onLeave = cachedLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .count();
        onLeaveLabel.setText(String.valueOf(onLeave));

        long pending = cachedLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING)
                .count();
        pendingRequestsLabel.setText(String.valueOf(pending));
    }

    private void updateDepartmentsChart() {
        Map<String, Long> deptCounts = cachedEmployees.stream()
                .filter(Employee::isActive)
                .collect(Collectors.groupingBy(
                        e -> (e.getDepartment() != null) ? e.getDepartment().getName() : "Unknown",
                        Collectors.counting()
                ));
        initChart(deptWebView, "chart_departments.html", mapToLabels(deptCounts), mapToData(deptCounts));
    }

    // 1. HIRES CHART UPDATE
    private void updateHiresChart(int year) {
        Map<Month, Long> hiresPerMonth = cachedEmployees.stream()
                .filter(e -> e.getHireDate() != null && e.getHireDate().getYear() == year)
                .collect(Collectors.groupingBy(e -> e.getHireDate().getMonth(), Collectors.counting()));

        String monthsLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";
        initChart(hiresWebView, "chart_hires.html", monthsLabels, getMonthlyDataString(hiresPerMonth));
    }

    // 2. LEAVES CHART UPDATE
    private void updateLeavesChart(int year) {
        Map<Month, Long> leavesPerMonth = cachedLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> l.getStartDate() != null && l.getStartDate().getYear() == year)
                .collect(Collectors.groupingBy(l -> l.getStartDate().getMonth(), Collectors.counting()));

        String monthsLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";
        initChart(leavesWebView, "chart_leaves.html", monthsLabels, getMonthlyDataString(leavesPerMonth));
    }

    // 3. ATTENDANCE CHART UPDATE
    private void updateAttendanceChart(int year) {
        // ΣΗΜΕΙΩΣΗ: Αφού διαλέγουμε έτος, δεν έχει νόημα να δείξουμε "Last 7 days".
        // Οπότε δείχνουμε "Attendance Records per Month" για το έτος που επιλέχθηκε.

        Map<Month, Long> attendancePerMonth = cachedAttendance.stream()
                .filter(a -> a.getDate().getYear() == year)
                .collect(Collectors.groupingBy(a -> a.getDate().getMonth(), Collectors.counting()));

        String monthsLabels = "['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']";

        // Χρησιμοποιούμε το chart_hires.html ή φτιάχνεις ένα copy (chart_attendance_year.html)
        // αν θες να αλλάξεις χρώματα, αλλά η δομή δεδομένων είναι ίδια (Bar Chart ανά μήνα).
        // Εδώ χρησιμοποιώ το "chart_attendance.html" αλλά του στέλνω μήνες αντί για μέρες.
        // ΠΡΟΣΟΧΗ: Το chart_attendance.html ίσως περιμένει μέρες.
        // Αν θες να φαίνεται σαν Bar Chart ανά μήνα, καλύτερα να χρησιμοποιήσεις τη λογική του chart_hires (Bar chart).

        // Για ευκολία εδώ χρησιμοποιώ τη λογική Bar Chart (όπως στα Hires)
        initChart(attendanceWebView, "chart_hires.html", monthsLabels, getMonthlyDataString(attendancePerMonth));
    }

    // --- HELPERS ---

    private void fadeOutLoading() {
        if (loadingOverlay != null) {
            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
                fadeOut.play();
            });
            delay.play();
        }
    }

    private void initChart(WebView webView, String htmlFile, String labels, String data) {
        WebEngine engine = webView.getEngine();
        try {
            URL resource = getClass().getResource("/charts/" + htmlFile);
            if (resource == null) {
                System.err.println("  Chart HTML not found: /charts/" + htmlFile);
                return;
            }

            // Φόρτωση μόνο αν δεν έχει ήδη φορτωθεί η σελίδα (για να αποφύγουμε το flickering στο update)
            // Αλλά επειδή αλλάζουμε δεδομένα, συχνά το reload είναι πιο ασφαλές για καθαρό chart.
            engine.load(resource.toExternalForm());

            engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    PauseTransition delay = new PauseTransition(Duration.seconds(0.8));
                    delay.setOnFinished(event -> {
                        try {
                            engine.executeScript("if (window.updateChart) { window.updateChart(" + labels + ", " + data + "); }");
                        } catch (Exception e) {
                            System.err.println("JS Error in " + htmlFile + ": " + e.getMessage());
                        }
                    });
                    delay.play();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMonthlyDataString(Map<Month, Long> dataMap) {
        StringBuilder sb = new StringBuilder("[");
        for (Month m : Month.values()) {
            sb.append(dataMap.getOrDefault(m, 0L)).append(",");
        }
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
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