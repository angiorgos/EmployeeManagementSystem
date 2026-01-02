package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.AttendanceService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Controller
public class AttendanceController implements Initializable {

    @Autowired private EmployeeService employeeService;
    @Autowired private AttendanceService attendanceService;

    // FXML Elements
    @FXML private VBox loadingOverlay; // <-- Σύνδεση με το Overlay
    @FXML private TextField searchField;
    @FXML private Label pcTimeLabel;
    @FXML private TableView<Employee> employeesTable;
    @FXML private TableColumn<Employee, Long> colId;
    @FXML private TableColumn<Employee, String> colFirstName;
    @FXML private TableColumn<Employee, String> colLastName;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, String> colLastIn;
    @FXML private TableColumn<Employee, String> colLastOut;

    @FXML private Label selectedNameLabel;
    @FXML private Label selectedDeptLabel;
    @FXML private Label selectedRoleLabel;
    @FXML private Button checkInBtn;
    @FXML private Button checkOutBtn;
    @FXML private Label todayInLabel;
    @FXML private Label todayOutLabel;
    @FXML private Label messageLabel;

    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredEmployees;
    private final Map<Long, Attendance> todayAttendanceMap = new HashMap<>();

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupSelection();
        startClock();
        loadData(); // <-- Εδώ ξεκινάει το Loading
    }

    // --- LOADING LOGIC WITH OVERLAY ---
    private void loadData() {
        // 1. Εμφάνιση Overlay
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        }

        // 2. Μικρή καθυστέρηση για να προλάβει να φανεί το Overlay
        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(ev -> {

            // 3. Background Task (Βάση Δεδομένων)
            Task<AttendanceDataPayload> task = new Task<>() {
                @Override
                protected AttendanceDataPayload call() throws Exception {
                    // Fetch Employees
                    List<Employee> empList = employeeService.getAllEmployees();
                    // Fetch Attendance
                    List<Attendance> attList = attendanceService.getAttendanceByDate(LocalDate.now());

                    return new AttendanceDataPayload(empList, attList);
                }
            };

            // 4. Success -> Update UI & Fade Out
            task.setOnSucceeded(e -> {
                AttendanceDataPayload data = task.getValue();

                // Ενημέρωση λιστών (στο UI Thread)
                employees.setAll(data.employees);

                todayAttendanceMap.clear();
                for (Attendance att : data.attendances) {
                    todayAttendanceMap.put(att.getEmployee().getId(), att);
                }

                employeesTable.refresh();

                // Fade Out Animation
                if (loadingOverlay != null) {
                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
                    fadeOut.play();
                }
            });

            task.setOnFailed(e -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                e.getSource().getException().printStackTrace();
                setMessage("Failed to load data.");
            });

            new Thread(task).start();
        });

        delay.play();
    }

    // Βοηθητική κλάση για μεταφορά δεδομένων από το Task
    private static class AttendanceDataPayload {
        List<Employee> employees;
        List<Attendance> attendances;

        public AttendanceDataPayload(List<Employee> employees, List<Attendance> attendances) {
            this.employees = employees;
            this.attendances = attendances;
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colFirstName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFirstName()));
        colLastName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLastName()));

        colDepartment.setCellValueFactory(c -> {
            if (c.getValue().getDepartment() != null)
                return new SimpleStringProperty(c.getValue().getDepartment().getName());
            return new SimpleStringProperty("-");
        });

        colLastIn.setCellValueFactory(c -> {
            Attendance att = todayAttendanceMap.get(c.getValue().getId());
            return new SimpleStringProperty(att != null && att.getCheckInTime() != null ? att.getCheckInTime().format(TIME_FMT) : "—");
        });

        colLastOut.setCellValueFactory(c -> {
            Attendance att = todayAttendanceMap.get(c.getValue().getId());
            return new SimpleStringProperty(att != null && att.getCheckOutTime() != null ? att.getCheckOutTime().format(TIME_FMT) : "—");
        });

        employeesTable.setPlaceholder(new Label("No employees found"));
    }

    private void setupSearch() {
        filteredEmployees = new FilteredList<>(employees, e -> true);
        employeesTable.setItems(filteredEmployees);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);
            filteredEmployees.setPredicate(emp -> {
                if (q.isEmpty()) return true;
                return String.valueOf(emp.getId()).contains(q) ||
                        safe(emp.getFirstName()).toLowerCase().contains(q) ||
                        safe(emp.getLastName()).toLowerCase().contains(q);
            });
        });
    }

    private void setupSelection() {
        checkInBtn.disableProperty().bind(Bindings.isNull(employeesTable.getSelectionModel().selectedItemProperty()));
        checkOutBtn.disableProperty().bind(Bindings.isNull(employeesTable.getSelectionModel().selectedItemProperty()));

        employeesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldEmp, newEmp) -> {
            clearMessage();
            if (newEmp == null) {
                selectedNameLabel.setText("—");
                selectedDeptLabel.setText("—");
                selectedRoleLabel.setText("—");
                todayInLabel.setText("Check In: —");
                todayOutLabel.setText("Check Out: —");
            } else {
                selectedNameLabel.setText(newEmp.getFirstName() + " " + newEmp.getLastName());
                selectedDeptLabel.setText(newEmp.getDepartment() != null ? "Dept: " + newEmp.getDepartment().getName() : "-");
                selectedRoleLabel.setText("Role: " + (newEmp.getUser() != null && newEmp.getUser().getRole() != null ? newEmp.getUser().getRole().getName() : "-"));
                refreshTodayPanel(newEmp);
            }
        });
    }

    private void refreshTodayPanel(Employee employee) {
        Attendance a = todayAttendanceMap.get(employee.getId());
        if (a == null) {
            todayInLabel.setText("Check In: —");
            todayOutLabel.setText("Check Out: —");
        } else {
            todayInLabel.setText("Check In: " + formatTimeOrDash(a.getCheckInTime()));
            todayOutLabel.setText("Check Out: " + formatTimeOrDash(a.getCheckOutTime()));
        }
    }

    @FXML private void onCheckIn() { handleAction(true); }
    @FXML private void onCheckOut() { handleAction(false); }

    private void handleAction(boolean isCheckIn) {
        Employee emp = employeesTable.getSelectionModel().getSelectedItem();
        if (emp == null) return;
        clearMessage();
        try {
            if (isCheckIn) attendanceService.checkIn(emp, LocalDate.now(), LocalTime.now());
            else attendanceService.checkOut(emp, LocalDate.now(), LocalTime.now());

            Attendance updated = attendanceService.getTodayOrNull(emp, LocalDate.now());
            todayAttendanceMap.put(emp.getId(), updated);

            setMessage(isCheckIn ? "Checked in successfully" : "Checked out successfully");
            refreshTodayPanel(emp);
            employeesTable.refresh();
        } catch (Exception ex) {
            setMessage(ex.getMessage());
        }
    }

    private void startClock() {
        new Timeline(new KeyFrame(Duration.seconds(1), e -> pcTimeLabel.setText(LocalDateTime.now().format(CLOCK_FMT)))).play();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String formatTimeOrDash(LocalTime t) { return t == null ? "—" : t.format(TIME_FMT); }

    private void setMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle(""); // Χωρίς χρώματα όπως ζήτησες
    }

    private void clearMessage() {
        messageLabel.setText("");
    }
}