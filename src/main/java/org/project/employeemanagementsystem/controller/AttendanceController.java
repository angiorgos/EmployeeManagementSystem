package org.project.employeemanagementsystem.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
import java.util.Locale;
import java.util.ResourceBundle;

@Controller
public class AttendanceController implements Initializable {

    @Autowired private EmployeeService employeeService;
    @Autowired private AttendanceService attendanceService;

    // ===== FXML (από το attendance.fxml που σου έγραψα) =====
    @FXML private TextField searchField;
    @FXML private Label pcTimeLabel;

    @FXML private TableView<Employee> employeesTable;
    @FXML private TableColumn<Employee, Long> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colStatus;
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

    // ===== State =====
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredEmployees;

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");

    private Timeline clockTimeline;

    private void setupTable() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));

        // 🔧 ΑΛΛΑΞΕ ΑΥΤΟ ΑΝ ΔΕΝ ΕΧΕΙΣ getFullName()
        colName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getEmployeeDisplayName(c.getValue())));

        // 🔧 Αν δεν έχεις Department/Role, άστα "—" ή άλλαξέ τα.
        colDepartment.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getEmployeeDepartmentName(c.getValue())));
        colRole.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getEmployeeRoleName(c.getValue())));

        employeesTable.setPlaceholder(new Label("No employees found"));
    }

    private void setupSearch() {
        filteredEmployees = new FilteredList<>(employees, e -> true);
        employeesTable.setItems(filteredEmployees);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);

            filteredEmployees.setPredicate(emp -> {
                if (q.isEmpty()) return true;

                String id = String.valueOf(emp.getId());
                String name = getEmployeeDisplayName(emp).toLowerCase(Locale.ROOT);
                String dept = getEmployeeDepartmentName(emp).toLowerCase(Locale.ROOT);
                String role = getEmployeeRoleName(emp).toLowerCase(Locale.ROOT);

                return id.contains(q) || name.contains(q) || dept.contains(q) || role.contains(q);
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
                return;
            }

            selectedNameLabel.setText(getEmployeeDisplayName(newEmp));
            selectedDeptLabel.setText("Department: " + getEmployeeDepartmentName(newEmp));
            selectedRoleLabel.setText("Role: " + getEmployeeRoleName(newEmp));

           // refreshTodayPanel(newEmp.getId());
        });
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                pcTimeLabel.setText(LocalDateTime.now().format(CLOCK_FMT))
        ));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /*private void loadEmployees() {
        employees.setAll(employeeService.findAllEmployees());
    }*/

    /*private void refreshTodayPanel(long employeeId) {
        Attendance a = attendanceService.getTodayOrNull(employeeId, LocalDate.now());

        if (a == null) {
            todayInLabel.setText("Check In: —");
            todayOutLabel.setText("Check Out: —");
        } else {
            todayInLabel.setText("Check In: " + formatTimeOrDash(a.getCheckInTime()));
            todayOutLabel.setText("Check Out: " + formatTimeOrDash(a.getCheckOutTime()));
        }

        employeesTable.refresh();
    }*/

    @FXML
    private void onCheckIn() {
        Employee emp = employeesTable.getSelectionModel().getSelectedItem();
        if (emp == null) return;

        clearMessage();

        /*try {
            attendanceService.checkIn(emp, LocalDate.now(), LocalTime.now());
            setMessage("Checked in: " + getEmployeeDisplayName(emp), false);
            refreshTodayPanel(emp.getId());
        } catch (IllegalStateException ex) {
            setMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            setMessage("Error during Check In.", true);
        }*/
    }

    @FXML
    private void onCheckOut() {
        Employee emp = employeesTable.getSelectionModel().getSelectedItem();
        if (emp == null) return;

        clearMessage();

        /*try {
            attendanceService.checkOut(emp, LocalDate.now(), LocalTime.now());
            setMessage("Checked out: " + getEmployeeDisplayName(emp), false);
            refreshTodayPanel(emp.getId());
        } catch (IllegalStateException ex) {
            setMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            setMessage("Error during Check Out.", true);
        }*/
    }

    // ================= helpers (προσαρμόζονται εύκολα) =================

    private String getEmployeeDisplayName(Employee e) {
        return e.getFirstName() + " " + e.getLastName();
    }


    private String getEmployeeDepartmentName(Employee e) {
        if (e.getDepartment() == null) return "—";
        return e.getDepartment().getName();
    }

    private String getEmployeeRoleName(Employee e) {
        return "—";
    }


    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String formatTimeOrDash(LocalTime t) {
        if (t == null) return "—";
        return t.format(TIME_FMT);
    }

    private void setMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("msg-error", "msg-success");
        messageLabel.getStyleClass().add(isError ? "msg-error" : "msg-success");
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("msg-error", "msg-success");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupSelection();
        startClock();
        //loadEmployees();
    }
}
