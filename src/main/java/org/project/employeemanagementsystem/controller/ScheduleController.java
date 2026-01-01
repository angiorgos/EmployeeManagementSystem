package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TabPane;

@Controller
public class ScheduleController implements Initializable {

    // ====== Services (backend) ======
    @Autowired private ScheduleService scheduleService;
    @Autowired private EmployeeService employeeService;
    @FXML private TabPane scheduleTabs;

    @FXML private BorderPane newScheduleView;              // inject-άρει το root node (BorderPane)
    @FXML private ScheduleController2 newScheduleViewController; // inject-άρει τον controller του include

    // ====== Calendar Tab (Overview) ======
    @FXML private Label monthLabel;
    @FXML private GridPane monthView;
    @FXML private Label selectedDayLabel;
    @FXML private ListView<String> workingEmployeesList;

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML private ToggleButton todayBtn;

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

        // ===== header: weekday (left, μόνο 1η σειρά) + day number (right, πάντα) =====
        HBox header = new HBox();
        header.setAlignment(Pos.TOP_LEFT);

        Label weekdayLabel = null;
        if (rowIndex == 0) {
            weekdayLabel = new Label(
                    date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
            );
            weekdayLabel.getStyleClass().add("weekday-label");
        }

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");

// spacer για να σπρώχνει το dayNumber δεξιά
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

// χτίσιμο header
        if (weekdayLabel != null) header.getChildren().add(weekdayLabel);
        header.getChildren().addAll(spacer, dayNumber);

// τα chips κάτω από το header
        VBox employeesBox = new VBox(2);

        // προσωρινό mock (μέχρι να το δέσεις με scheduleService)
        boolean weekday = date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
        if (inMonth && weekday) {
            employeesBox.getChildren().add(employeeChip("Maria P."));
            employeesBox.getChildren().add(employeeChip("Giorgos K."));
            employeesBox.getChildren().add(employeeChip("Dimitris A."));
        }

        root.getChildren().addAll(header, employeesBox);

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

        // Calendar init (αν υπάρχουν τα fx:id στο FXML)
        if (monthView != null && monthLabel != null) {
            renderMonth(currentMonth);
            updateRightPanel(selectedDate);
            syncTodayToggle();
        }
        // ✅ ΠΑΙΡΝΟΥΜΕ ΤΟΝ CONTROLLER ΤΟΥ schedule2.fxml
        if (newScheduleViewController != null) {
            newScheduleViewController.setOnScheduleSaved(() -> {
                renderMonth(currentMonth);
                updateRightPanel(selectedDate);
                syncTodayToggle();

                // γύρνα στο Overview tab
                if (scheduleTabs != null) {
                    scheduleTabs.getSelectionModel().select(0);
                }
            });
        }

    }
}