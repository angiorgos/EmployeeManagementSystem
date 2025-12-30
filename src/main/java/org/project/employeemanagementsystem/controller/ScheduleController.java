package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Schedule;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.ResourceBundle;

@Controller
public class ScheduleController implements Initializable {

    // ====== Services (backend) ======
    @Autowired private ScheduleService scheduleService;
    @Autowired private EmployeeService employeeService;

    // ====== Calendar Tab (Overview) ======
    @FXML private Label monthLabel;
    @FXML private GridPane monthView;
    @FXML private Label selectedDayLabel;
    @FXML private ListView<String> workingEmployeesList;

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML private ToggleButton todayBtn;
    // ====== New Schedule Tab ======
    @FXML private TableView<Schedule> scheduleTable;
    @FXML private ComboBox<Employee> employeeComboBox;

    private void syncTodayToggle() {
        if (todayBtn == null) return;
        todayBtn.setSelected(selectedDate.equals(LocalDate.now()));
    }

    // ===== Buttons =====
    @FXML
    private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth(currentMonth);
    }

    @FXML
    private void onPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth(currentMonth);
    }

    @FXML
    private void onToday() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        renderMonth(currentMonth);
        updateRightPanel(selectedDate);
        syncTodayToggle();
    }

    // ===== Calendar render (Monday-first) =====
    private void renderMonth(YearMonth ym) {
        monthView.getChildren().clear();
        monthLabel.setText(capitalize(ym.getMonth().toString()) + " " + ym.getYear());

        LocalDate firstOfMonth = ym.atDay(1);

        // Monday-first: Mon=0..Sun=6
        int shift = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate gridStart = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) {
            LocalDate date = gridStart.plusDays(i);
            int row = i / 7;
            int col = i % 7;

            VBox cell = createDayCell(date, ym, row);
            monthView.add(cell, col, row);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
    }

    private VBox createDayCell(LocalDate date, YearMonth visibleMonth, int rowIndex) {
        VBox root = new VBox(4);
        root.getStyleClass().add("day-cell");
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setFillWidth(true);

        boolean inMonth = date.getMonth().equals(visibleMonth.getMonth());

        // Weekday label μόνο στην πρώτη σειρά (αν θες αυτό το look)
        if (rowIndex == 0) {
            Label weekdayLabel = new Label(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
            weekdayLabel.getStyleClass().add("weekday-label");
            weekdayLabel.setMaxWidth(Double.MAX_VALUE);
            root.getChildren().add(weekdayLabel);
        }

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");
        dayNumber.setMaxWidth(Double.MAX_VALUE);

        VBox employeesBox = new VBox(2);

        // προσωρινό mock (μέχρι να το δέσεις με scheduleService)
        boolean weekday = date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
        if (inMonth && weekday) {
            employeesBox.getChildren().add(employeeChip("Maria P."));
            employeesBox.getChildren().add(employeeChip("Giorgos K."));
            employeesBox.getChildren().add(employeeChip("Dimitris A."));
        }

        root.getChildren().addAll(dayNumber, employeesBox);

        if (!inMonth) root.getStyleClass().add("out-month");
        if (date.equals(selectedDate)) root.getStyleClass().add("selected-day");

        root.setOnMouseClicked(e -> {
            selectedDate = date;
            updateRightPanel(date);
            renderMonth(currentMonth);
            syncTodayToggle();
        });

        return root;
    }

    private Label employeeChip(String name) {
        Label l = new Label(name);
        l.getStyleClass().add("employee-chip");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private void updateRightPanel(LocalDate date) {
        if (selectedDayLabel != null) {
            selectedDayLabel.setText(date.toString());
        }
        if (workingEmployeesList != null) {
            // προσωρινό mock
            workingEmployeesList.getItems().setAll(
                    "Maria P. (09:00 - 17:00)",
                    "Giorgos K. (09:00 - 17:00)",
                    "Dimitris A. (12:00 - 20:00)"
            );
        }
    }

    private String capitalize(String s) {
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1) Calendar init (αν υπάρχουν τα fx:id στο FXML)
        if (monthView != null && monthLabel != null) {
            renderMonth(currentMonth);
            updateRightPanel(selectedDate);
            syncTodayToggle();
        }

        // 2) New Schedule tab init (αν υπάρχουν τα fx:id στο FXML)
        if (scheduleTable != null) {
            scheduleTable.setItems(FXCollections.observableArrayList(scheduleService.getAllSchedules()));
        }

        if (employeeComboBox != null) {
            employeeComboBox.getItems().setAll(employeeService.getActiveEmployees());

            // πολύ χρήσιμο: πώς θα εμφανίζεται ο Employee στο dropdown
            employeeComboBox.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Employee item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getFirstName() + " " + item.getLastName());
                }
            });
            employeeComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Employee item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getFirstName() + " " + item.getLastName());
                }
            });
        }
    }
}