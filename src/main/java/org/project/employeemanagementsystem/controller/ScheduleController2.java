package org.project.employeemanagementsystem.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Controller
public class ScheduleController2 implements Initializable {

    // ====== FXML ======
    @FXML private GridPane weekGrid;
    @FXML private ComboBox<String> employeeComboBox;
    @FXML private Button submitBtn;
    @FXML private Button deleteFromScheduleBtn;
    @FXML private DatePicker validFromPicker;
    @FXML private DatePicker validToPicker;

    // Time Pickers
    @FXML private ComboBox<String> monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd;

    // Data & State
    private EmployeeService employeeService;
    private final ObservableList<String> allEmployeesNames = FXCollections.observableArrayList();
    private final Map<String, VBox> cellMap = new HashMap<>(); // Χάρτης για τα κελιά του Grid
    private final List<Label> previewChips = new ArrayList<>(); // Λίστα για τα chips προεπισκόπησης

    // Grid Settings
    private static final DayOfWeek[] DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private final int startHour = 9;
    private final int endHour = 21;

    // DTO Class
    public static class WeekSchedulePayload {
        public final String employee;
        public final Map<DayOfWeek, LocalTime[]> ranges;
        public final LocalDate validFrom;
        public final LocalDate validTo;

        public WeekSchedulePayload(String employee, Map<DayOfWeek, LocalTime[]> ranges, LocalDate validFrom, LocalDate validTo) {
            this.employee = employee;
            this.ranges = ranges;
            this.validFrom = validFrom;
            this.validTo = validTo;
        }
    }

    private Consumer<WeekSchedulePayload> onScheduleSaved;
    private Consumer<String> onScheduleDeleted;

    // =========================================================
    // INITIALIZATION
    // =========================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (weekGrid == null) return;

        // 1. Setup UI & Search
        if (employeeComboBox != null) setupEmployeeSearch();
        installEmployeePlaceholder();
        renderWeekGrid(); // Δημιουργεί το Grid και γεμίζει το cellMap

        // 2. Setup Time Pickers
        ObservableList<String> times = buildTimes();
        ComboBox[] allBoxes = {monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd};
        for (ComboBox cb : allBoxes) {
            if (cb == null) continue;
            cb.setItems(times);
            makeBlankSelectable(cb);
            attachAutoPreview(cb); // Συνδέει το listener για το live preview
        }

        // 3. Time Constraints (End > Start)
        enforceEndAfterStart(monStart, monEnd);
        enforceEndAfterStart(tueStart, tueEnd);
        enforceEndAfterStart(wedStart, wedEnd);
        enforceEndAfterStart(thuStart, thuEnd);
        enforceEndAfterStart(friStart, friEnd);
        enforceEndAfterStart(satStart, satEnd);
        enforceEndAfterStart(sunStart, sunEnd);

        // 4. Week Selection Logic
        setupWeekSnapping();

        // 5. Auto-fill Current Week
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (validFromPicker != null) {
            validFromPicker.setValue(thisMonday);
        }

        // 6. Listeners for UI State (Enable/Disable Buttons)
        if (employeeComboBox != null) {
            employeeComboBox.valueProperty().addListener((obs, oldV, newV) -> {
                boolean ok = newV != null && !newV.isBlank();
                setInputsEnabled(ok);
                updateSubmitEnabled();
                updateDeleteEnabled();
                if (!ok) clearPreview();
                else refreshPreviewFromPickers(); // Αν επιλεγεί υπάλληλος, δείξε τυχόν επιλεγμένες ώρες.
            });
        }

        if (validFromPicker != null) validFromPicker.valueProperty().addListener((obs, o, n) -> updateSubmitEnabled());

        // Initial State
        setInputsEnabled(false);
        updateSubmitEnabled();
        updateDeleteEnabled();
    }

    // =========================================================
    // GRID & PREVIEW LOGIC
    // =========================================================

    private void renderWeekGrid() {
        weekGrid.getChildren().clear();
        cellMap.clear();

        int rows = (endHour - startHour) + 1; // 13 γραμμές (9-21)

        for (int r = 0; r < rows; r++) {
            int hour = startHour + r;
            for (int c = 0; c < 7; c++) {
                DayOfWeek day = DAYS[c];

                VBox cell = new VBox();
                cell.getStyleClass().add("day-cell");
                cell.setMinSize(0, 0);
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                weekGrid.add(cell, c, r);
                GridPane.setHgrow(cell, Priority.ALWAYS);
                GridPane.setVgrow(cell, Priority.ALWAYS);

                // Αποθήκευση στο cellMap
                cellMap.put(key(day, hour), cell);
            }
        }
    }

    private void refreshPreviewFromPickers() {
        clearPreview();
        if (employeeComboBox == null) return;
        String employee = employeeComboBox.getValue();
        if (employee == null || employee.isBlank()) return;

        applyDayRange(DayOfWeek.MONDAY, monStart, monEnd, employee);
        applyDayRange(DayOfWeek.TUESDAY, tueStart, tueEnd, employee);
        applyDayRange(DayOfWeek.WEDNESDAY, wedStart, wedEnd, employee);
        applyDayRange(DayOfWeek.THURSDAY, thuStart, thuEnd, employee);
        applyDayRange(DayOfWeek.FRIDAY, friStart, friEnd, employee);
        applyDayRange(DayOfWeek.SATURDAY, satStart, satEnd, employee);
        applyDayRange(DayOfWeek.SUNDAY, sunStart, sunEnd, employee);
    }

    private void applyDayRange(DayOfWeek day, ComboBox<String> startCb, ComboBox<String> endCb, String employee) {
        if (startCb == null || endCb == null) return;
        LocalTime start = parseTime(startCb.getValue());
        LocalTime end = parseTime(endCb.getValue());

        if (start == null || end == null) return;
        if (!end.isAfter(start)) return;

        int startRow = start.getHour() - startHour;
        int endRowExclusive = end.getHour() - startHour;
        if (end.getMinute() > 0) endRowExclusive++;

        startRow = Math.max(0, startRow);
        endRowExclusive = Math.min((endHour - startHour) + 1, endRowExclusive);

        for (int r = startRow; r < endRowExclusive; r++) {
            addChipToCell(day, r, employee);
        }
    }

    private void addChipToCell(DayOfWeek day, int hourRow, String employee) {
        int hour = startHour + hourRow;
        VBox cell = cellMap.get(key(day, hour));

        if (cell == null) return;

        Label chip = new Label(employee);
        chip.getStyleClass().add("employee-chip");
        chip.setMaxWidth(Double.MAX_VALUE);

        cell.getChildren().add(chip);
        previewChips.add(chip);
    }

    private void clearPreview() {
        for (Label chip : previewChips) {
            if (chip.getParent() instanceof VBox) {
                ((VBox) chip.getParent()).getChildren().remove(chip);
            }
        }
        previewChips.clear();
    }

    private String key(DayOfWeek day, int hour) {
        return day + "_" + hour;
    }

    private void attachAutoPreview(ComboBox<String> cb) {
        if (cb == null) return;
        cb.valueProperty().addListener((obs, o, n) -> refreshPreviewFromPickers());
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private void setupWeekSnapping() {
        if (validFromPicker == null || validToPicker == null) return;

        validToPicker.setDisable(true);
        if (!validToPicker.getStyleClass().contains("date-picker-readonly")) {
            validToPicker.getStyleClass().add("date-picker-readonly");
        }

        validFromPicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate == null) {
                validToPicker.setValue(null);
                return;
            }
            LocalDate startOfWeek = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            if (!newDate.equals(startOfWeek)) {
                Platform.runLater(() -> validFromPicker.setValue(startOfWeek));
            } else {
                validToPicker.setValue(endOfWeek);
            }
        });
    }

    public void setEmployeeService(EmployeeService employeeService) {
        this.employeeService = employeeService;
        loadEmployees();
    }

    private void loadEmployees() {
        if (employeeService != null) {
            List<Employee> employees = employeeService.getAllEmployees();
            List<String> names = employees.stream()
                    .map(e -> e.getLastName() + " " + e.getFirstName())
                    .sorted()
                    .collect(Collectors.toList());

            allEmployeesNames.setAll(names);
            if (employeeComboBox != null) employeeComboBox.setItems(allEmployeesNames);
        }
    }

    private void setupEmployeeSearch() {
        if (employeeComboBox == null) return;
        employeeComboBox.setEditable(true);
        employeeComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.equals(employeeComboBox.getSelectionModel().getSelectedItem())) return;
            if (newText == null || newText.isEmpty()) employeeComboBox.setItems(allEmployeesNames);
            else {
                String search = newText.toLowerCase();
                List<String> filtered = allEmployeesNames.stream().filter(n -> n.toLowerCase().contains(search)).collect(Collectors.toList());
                employeeComboBox.setItems(FXCollections.observableArrayList(filtered));
                if (!employeeComboBox.isShowing()) employeeComboBox.show();
            }
        });
    }

    private void installEmployeePlaceholder() {
        if (employeeComboBox == null) return;
        employeeComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "Choose Employee" : item);
            }
        });
    }

    public void setOnScheduleSaved(Consumer<WeekSchedulePayload> onScheduleSaved) { this.onScheduleSaved = onScheduleSaved; }
    public void setOnScheduleDeleted(Consumer<String> onScheduleDeleted) { this.onScheduleDeleted = onScheduleDeleted; }

    @FXML private void onSubmit() {
        WeekSchedulePayload payload = buildPayload();
        if (payload != null && onScheduleSaved != null) {
            onScheduleSaved.accept(payload);
        }
        clearPreview();
        clearInputs();
    }

    @FXML private void onDeleteFromSchedule() {
        String emp = (employeeComboBox != null) ? employeeComboBox.getValue() : null;
        if (emp != null && !emp.isBlank() && onScheduleDeleted != null) {
            onScheduleDeleted.accept(emp);
        }
    }

    private void clearInputs() {
        ComboBox[] allBoxes = {monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd};
        for (ComboBox cb : allBoxes) {
            if (cb != null) {
                cb.getSelectionModel().clearSelection();
                cb.setValue("");
            }
        }
        if (employeeComboBox != null) {
            employeeComboBox.getSelectionModel().clearSelection();
            employeeComboBox.setValue(null);
        }
        setInputsEnabled(false);
        updateSubmitEnabled();
    }

    private void setInputsEnabled(boolean enabled) {
        boolean disable = !enabled;
        ComboBox[] allBoxes = {monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd};
        for (ComboBox cb : allBoxes) { if (cb != null) cb.setDisable(disable); }
        if (validFromPicker != null) validFromPicker.setDisable(disable);
        updateSubmitEnabled();
        updateDeleteEnabled();
    }

    private void updateSubmitEnabled() {
        if (submitBtn == null) return;
        String emp = (employeeComboBox != null) ? employeeComboBox.getValue() : null;
        LocalDate from = (validFromPicker != null) ? validFromPicker.getValue() : null;
        LocalDate to = (validToPicker != null) ? validToPicker.getValue() : null;
        boolean ok = emp != null && !emp.isBlank() && from != null && to != null && !to.isBefore(from);
        submitBtn.setDisable(!ok);
    }

    private void updateDeleteEnabled() {
        if (deleteFromScheduleBtn == null) return;
        String emp = (employeeComboBox != null) ? employeeComboBox.getValue() : null;
        deleteFromScheduleBtn.setDisable(emp == null || emp.isBlank());
    }

    private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private ObservableList<String> buildTimes() {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        // ΑΛΛΑΓΗ: plusHours(1) αντί για plusMinutes(30)
        for (LocalTime t = LocalTime.of(9, 0); !t.isAfter(LocalTime.of(21, 0)); t = t.plusHours(1)) {
            items.add(t.format(TIME_12H));
        }
        return items;
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalTime.parse(s, TIME_12H);
    }

    private void makeBlankSelectable(ComboBox<String> cb) {
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });
    }

    private void enforceEndAfterStart(ComboBox<String> startCb, ComboBox<String> endCb) {
        if (startCb == null || endCb == null) return;
        endCb.valueProperty().addListener((obs, oldV, newV) -> {
            LocalTime start = parseTime(startCb.getValue());
            LocalTime end   = parseTime(newV);
            if (start == null || end == null) return;
            if (!end.isAfter(start)) {
                endCb.setValue(oldV);
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setHeaderText("Invalid time range");
                a.setContentText("End time must be later than Start time.");
                a.showAndWait();
            }
        });
    }

    private WeekSchedulePayload buildPayload() {
        String emp = (employeeComboBox != null) ? employeeComboBox.getValue() : null;
        if (emp == null || emp.isBlank()) return null;
        if (!allEmployeesNames.contains(emp)) return null;

        LocalDate from = (validFromPicker != null) ? validFromPicker.getValue() : null;
        LocalDate to = (validToPicker != null) ? validToPicker.getValue() : null;

        if (from == null || to == null) return null;
        if (to.isBefore(from)) return null;

        Map<DayOfWeek, LocalTime[]> map = new EnumMap<>(DayOfWeek.class);
        putRange(map, DayOfWeek.MONDAY, monStart, monEnd);
        putRange(map, DayOfWeek.TUESDAY, tueStart, tueEnd);
        putRange(map, DayOfWeek.WEDNESDAY, wedStart, wedEnd);
        putRange(map, DayOfWeek.THURSDAY, thuStart, thuEnd);
        putRange(map, DayOfWeek.FRIDAY, friStart, friEnd);
        putRange(map, DayOfWeek.SATURDAY, satStart, satEnd);
        putRange(map, DayOfWeek.SUNDAY, sunStart, sunEnd);

        if (map.isEmpty()) return null;

        return new WeekSchedulePayload(emp, map, from, to);
    }

    private void putRange(Map<DayOfWeek, LocalTime[]> map, DayOfWeek day, ComboBox<String> s, ComboBox<String> e) {
        LocalTime start = (s != null) ? parseTime(s.getValue()) : null;
        LocalTime end = (e != null) ? parseTime(e.getValue()) : null;
        if (start == null || end == null) return;
        if (!end.isAfter(start)) return;
        map.put(day, new LocalTime[]{start, end});
    }
}