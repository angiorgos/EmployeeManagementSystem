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
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TabPane;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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
    private final Map<LocalDate, List<String>> memorySchedules = new HashMap<>();

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML private ToggleButton todayBtn;

    private void syncTodayToggle() {
        if (todayBtn == null) return;
        todayBtn.setSelected(selectedDate.equals(LocalDate.now()));
    }

    private LocalDate weekStart(LocalDate any) {
        return any.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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

        List<String> lines = memorySchedules.getOrDefault(date, List.of());
        for (String s : lines) {
            String name = s.contains(" (") ? s.substring(0, s.indexOf(" (")) : s;
            employeesBox.getChildren().add(employeeChip(name));
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
        if (selectedDayLabel != null) selectedDayLabel.setText(date.toString());

        if (workingEmployeesList != null) {
            List<String> lines = memorySchedules.getOrDefault(date, List.of());
            if (lines.isEmpty()) {
                workingEmployeesList.getItems().setAll("No schedules for this day yet");
            } else {
                workingEmployeesList.getItems().setAll(lines);
            }
        }
    }

    private String capitalize(String s) {
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void onNewScheduleSaved(ScheduleController2.WeekSchedulePayload payload) {
        if (payload == null) return;

        String emp = payload.employee;
        LocalDate from = payload.validFrom;
        LocalDate to = payload.validTo;

        // 1) CLEAR: σβήσε τον employee από ΟΛΟ το διάστημα (ώστε να μην διπλομπαίνει)
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) { // inclusive
            removeEmployeeFromDate(d, emp);
        }

        // 2) APPLY: γράψε το νέο πρόγραμμα (μόνο στις μέρες που υπάρχουν ranges)
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) { // inclusive
            DayOfWeek dow = d.getDayOfWeek();
            LocalTime[] range = payload.ranges.get(dow);
            if (range == null) continue;

            LocalTime st = range[0];
            LocalTime en = range[1];

            String line = emp + " (" + st + " - " + en + ")";
            memorySchedules.computeIfAbsent(d, x -> new ArrayList<>()).add(line);
        }

        // refresh UI
        renderMonth(currentMonth);
        updateRightPanel(selectedDate);
        syncTodayToggle();

        if (scheduleTabs != null) scheduleTabs.getSelectionModel().select(0);
    }


    private void removeEmployeeFromDate(LocalDate date, String employee) {
        List<String> lines = memorySchedules.get(date);
        if (lines == null) return;

        // Σβήσε όλες τις γραμμές που ξεκινάνε με "employee ("
        lines.removeIf(s -> s != null && s.startsWith(employee + " ("));

        if (lines.isEmpty()) {
            memorySchedules.remove(date);
        }
    }

    private void onDeleteEmployeeSchedule(String employee) {
        if (employee == null || employee.isBlank()) return;

        // 1) Έλεγξε αν υπάρχει έστω ένα entry για αυτόν
        boolean exists = memorySchedules.values().stream()
                .anyMatch(lines -> lines.stream().anyMatch(l -> l.startsWith(employee + " (")));

        if (!exists) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("No schedule found");
            a.setContentText("There is no schedule saved for " + employee + ".");
            a.showAndWait();
            return;
        }

        // 2) Επιβεβαίωση (προαιρετικό αλλά ωραίο)
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete schedule");
        confirm.setContentText("Delete all saved schedule entries for " + employee + "?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        // 3) Σβήσιμο από όλες τις μέρες
        Iterator<Map.Entry<LocalDate, List<String>>> it = memorySchedules.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LocalDate, List<String>> e = it.next();
            List<String> lines = e.getValue();
            lines.removeIf(l -> l.startsWith(employee + " ("));
            if (lines.isEmpty()) it.remove();
        }

        // 4) Refresh UI
        renderMonth(currentMonth);
        updateRightPanel(selectedDate);
        syncTodayToggle();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setHeaderText("Schedule deleted");
        done.setContentText("Schedule for " + employee + " has been deleted.");
        done.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Calendar init (αν υπάρχουν τα fx:id στο FXML)
        if (monthView != null && monthLabel != null) {
            renderMonth(currentMonth);
            updateRightPanel(selectedDate);
            syncTodayToggle();
        }
        // ΠΑΙΡΝΟΥΜΕ ΤΟΝ CONTROLLER ΤΟΥ schedule2.fxml
        if (newScheduleViewController != null) {
            if (newScheduleViewController != null) {
                newScheduleViewController.setOnScheduleSaved(this::onNewScheduleSaved);
                newScheduleViewController.setOnScheduleDeleted(this::onDeleteEmployeeSchedule);
            }

        }

    }
}