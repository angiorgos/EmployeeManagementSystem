package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Schedule;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ScheduleController implements Initializable {

    @Autowired private ScheduleService scheduleService;
    @Autowired private EmployeeService employeeService;

    // --- FXML ELEMENTS ---
    @FXML private TabPane scheduleTabs;
    @FXML private BorderPane newScheduleView;
    @FXML private ScheduleController2 newScheduleViewController;
    @FXML private VBox loadingOverlay;

    // Calendar Elements
    @FXML private Label monthLabel;
    @FXML private GridPane monthView;
    @FXML private Label selectedDayLabel;
    @FXML private ListView<String> workingEmployeesList;

    // Navigation
    @FXML private ToggleButton todayBtn;
    @FXML private ToggleButton monthBtn;
    @FXML private ToggleButton weekBtn;

    // --- STATE ---
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private LocalDate selectedDate = LocalDate.now();
    private String currentViewMode = "MONTH";

    // *** Η ΒΑΣΙΚΗ ΑΛΛΑΓΗ: Κρατάμε ΟΛΑ τα δεδομένα στη μνήμη ***
    private List<Schedule> allSchedulesInMemory = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup Toggles
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                currentViewMode = "MONTH";
                refreshLocalView(); // <-- Τοπικό φιλτράρισμα
            });
            monthBtn.setSelected(true);
        }

        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                currentViewMode = "WEEK";
                currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                refreshLocalView(); // <-- Τοπικό φιλτράρισμα
            });
        }

        if (newScheduleViewController != null) {
            newScheduleViewController.setOnScheduleSaved(this::onNewScheduleSaved);
            newScheduleViewController.setOnScheduleDeleted(this::onDeleteEmployeeSchedule);
            newScheduleViewController.setEmployeeService(employeeService);
        }

        // 1. Φόρτωση από Βάση ΜΟΝΟ στην αρχή
        loadDataFromDB();
    }

    // =========================================================
    // 1. FETCH FROM DB (ΜΕ LOADING OVERLAY)
    // Καλέστε το ΜΟΝΟ στην αρχή ή μετά από Save/Delete
    // =========================================================
    private void loadDataFromDB() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        }

        // Delay για να φανεί το loading
        PauseTransition uiRenderDelay = new PauseTransition(Duration.millis(50));
        uiRenderDelay.setOnFinished(ev -> {

            Task<List<Schedule>> task = new Task<>() {
                @Override
                protected List<Schedule> call() throws Exception {
                    // Τραβάμε ΤΑ ΠΑΝΤΑ (ή π.χ. του τρέχοντος έτους αν είναι πολλά)
                    // Εδώ υποθέτουμε ότι τα φέρνουμε όλα για να έχουμε γρήγορο navigation
                    return scheduleService.getAllSchedules();
                }
            };

            task.setOnSucceeded(e -> {
                // Ανανέωση μνήμης
                allSchedulesInMemory = task.getValue();

                // Ανανέωση UI από τη μνήμη
                refreshLocalView();

                // Fade Out
                fadeOutOverlay();
            });

            task.setOnFailed(e -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                e.getSource().getException().printStackTrace();
            });

            new Thread(task).start();
        });

        uiRenderDelay.play();
    }

    private void fadeOutOverlay() {
        if (loadingOverlay == null) return;
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.4), loadingOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
        fadeOut.play();
    }

    // =========================================================
    // 2. REFRESH LOCAL VIEW (ΧΩΡΙΣ DB CALL - INSTANT)
    // Καλέστε το σε κάθε κουμπί πλοήγησης (Next/Prev/Week/Month)
    // =========================================================
    private void refreshLocalView() {
        if ("MONTH".equals(currentViewMode)) {
            renderMonthGrid();
        } else {
            renderWeekGrid();
        }
        updateRightPanel(selectedDate);
    }

    // --- NAVIGATION ACTIONS (Καλούν το LOCAL REFRESH) ---

    @FXML private void onNextMonth() {
        if ("MONTH".equals(currentViewMode)) currentMonth = currentMonth.plusMonths(1);
        else currentWeekStart = currentWeekStart.plusWeeks(1);

        refreshLocalView(); // <-- Ακαριαίο
    }

    @FXML private void onPrevMonth() {
        if ("MONTH".equals(currentViewMode)) currentMonth = currentMonth.minusMonths(1);
        else currentWeekStart = currentWeekStart.minusWeeks(1);

        refreshLocalView(); // <-- Ακαριαίο
    }

    @FXML private void onToday() {
        selectedDate = LocalDate.now();
        currentMonth = YearMonth.now();
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if(todayBtn != null) todayBtn.setSelected(false);

        refreshLocalView(); // <-- Ακαριαίο
    }

    // --- RENDER LOGIC (Χρησιμοποιεί το allSchedulesInMemory) ---

    private void renderMonthGrid() {
        monthView.getChildren().clear();
        monthLabel.setText(capitalize(currentMonth.getMonth().toString()) + " " + currentMonth.getYear());

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate gridStart = firstOfMonth.minusDays(shift);

        // Ομαδοποίηση από τη μνήμη (Πολύ γρήγορο)
        Map<LocalDate, List<Schedule>> scheduleMap = allSchedulesInMemory.stream()
                .collect(Collectors.groupingBy(Schedule::getDate));

        for (int i = 0; i < 42; i++) {
            LocalDate date = gridStart.plusDays(i);
            int row = i / 7;
            int col = i % 7;
            VBox cell = createDayCell(date, scheduleMap.get(date), currentMonth.getMonth());
            monthView.add(cell, col, row);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
    }

    private void renderWeekGrid() {
        monthView.getChildren().clear();
        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        monthLabel.setText(currentWeekStart.toString() + " - " + endOfWeek.toString());

        Map<LocalDate, List<Schedule>> scheduleMap = allSchedulesInMemory.stream()
                .collect(Collectors.groupingBy(Schedule::getDate));

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            VBox cell = createDayCell(date, scheduleMap.get(date), date.getMonth());
            monthView.add(cell, i, 0, 1, 6);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
    }

    private VBox createDayCell(LocalDate date, List<Schedule> dailySchedules, Month visibleMonth) {
        VBox root = new VBox();
        root.getStyleClass().add("day-cell");
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        boolean inMonth = date.getMonth().equals(visibleMonth);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.TOP_LEFT);
        header.setPadding(new javafx.geometry.Insets(2, 4, 0, 4));

        Label weekdayLabel = new Label(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
        weekdayLabel.getStyleClass().add("weekday-label");

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");
        dayNumber.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if ("WEEK".equals(currentViewMode)) {
            header.getChildren().addAll(weekdayLabel, spacer, dayNumber);
        } else {
            header.getChildren().addAll(spacer, dayNumber);
        }

        // Chips
        VBox employeesBox = new VBox(2);
        employeesBox.setPadding(new javafx.geometry.Insets(2));
        if (dailySchedules != null) {
            for (Schedule s : dailySchedules) {
                String name = s.getEmployee().getLastName();
                employeesBox.getChildren().add(employeeChip(name, s.getStartTime(), s.getEndTime()));
            }
        }

        // ScrollPane Fix
        ScrollPane scrollPane = new ScrollPane(employeesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("cell-scroll-pane");

        root.getChildren().addAll(header, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        if (!inMonth && "MONTH".equals(currentViewMode)) root.getStyleClass().add("out-month");
        if (date.equals(selectedDate)) root.getStyleClass().add("selected-day");

        // Click Event -> Καλεί το Local Refresh (όχι DB)
        root.setOnMouseClicked(e -> {
            selectedDate = date;
            updateRightPanel(date);
            refreshLocalView(); // Απλά ξαναζωγραφίζει το grid για να δείξει το selection border
        });

        return root;
    }

    private Label employeeChip(String name, LocalTime start, LocalTime end) {
        Label l = new Label(name + " (" + start + ")");
        l.getStyleClass().add("employee-chip");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private void updateRightPanel(LocalDate date) {
        if (selectedDayLabel != null) selectedDayLabel.setText(date.toString());
        if (workingEmployeesList != null) {
            // Φιλτράρισμα από τη μνήμη
            List<Schedule> daily = allSchedulesInMemory.stream()
                    .filter(s -> s.getDate().equals(date))
                    .collect(Collectors.toList());

            if (daily.isEmpty()) {
                workingEmployeesList.getItems().setAll("No schedules");
            } else {
                List<String> info = daily.stream()
                        .map(s -> s.getEmployee().getFirstName() + " " + s.getEmployee().getLastName() +
                                "\n" + s.getStartTime() + " - " + s.getEndTime())
                        .collect(Collectors.toList());
                workingEmployeesList.getItems().setAll(info);
            }
        }
    }

    // --- SAVE LOGIC (DB Call + Overlay + Refresh Cache) ---

    private void onNewScheduleSaved(ScheduleController2.WeekSchedulePayload payload) {
        if (payload == null) return;

        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        }

        PauseTransition uiRenderDelay = new PauseTransition(Duration.millis(50));
        uiRenderDelay.setOnFinished(ev -> {

            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String empName = payload.employee;
                    Employee employee = findEmployeeByString(empName);
                    if (employee == null) throw new Exception("Employee not found");

                    LocalDate from = payload.validFrom;
                    LocalDate to = payload.validTo;

                    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                        LocalDate currentDate = d;

                        List<Schedule> existing = scheduleService.getAllSchedules().stream()
                                .filter(s -> s.getDate().equals(currentDate) && s.getEmployee().getId().equals(employee.getId()))
                                .collect(Collectors.toList());
                        for(Schedule s : existing) scheduleService.deleteSchedule(s);

                        DayOfWeek dow = currentDate.getDayOfWeek();
                        LocalTime[] range = payload.ranges.get(dow);
                        if (range == null) continue;

                        Schedule newSchedule = new Schedule();
                        newSchedule.setEmployee(employee);
                        newSchedule.setDate(currentDate);
                        newSchedule.setStartTime(range[0]);
                        newSchedule.setEndTime(range[1]);
                        scheduleService.validateAndSave(newSchedule);
                    }
                    return null;
                }
            };

            saveTask.setOnSucceeded(e -> {
                // Μόλις σώσουμε, πρέπει να ξανατραβήξουμε τα data για να ενημερωθεί η μνήμη
                // Εδώ καλούμε το loadDataFromDB() αλλά χωρίς το delay γιατί ήδη βλέπουμε το overlay
                reloadCacheInBackground();
            });

            saveTask.setOnFailed(e -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save: " + saveTask.getException().getMessage());
                alert.show();
            });

            new Thread(saveTask).start();
        });
        uiRenderDelay.play();
    }

    // Βοηθητική μέθοδος για Refresh μετά από Save (χωρίς να αναβοσβήσει το overlay)
    private void reloadCacheInBackground() {
        Task<List<Schedule>> reloadTask = new Task<>() {
            @Override protected List<Schedule> call() {
                return scheduleService.getAllSchedules();
            }
        };

        reloadTask.setOnSucceeded(evt -> {
            allSchedulesInMemory = reloadTask.getValue();
            refreshLocalView();
            scheduleTabs.getSelectionModel().select(0);
            fadeOutOverlay();
        });

        new Thread(reloadTask).start();
    }

    private Employee findEmployeeByString(String comboValue) {
        List<Employee> all = employeeService.getAllEmployees();
        for (Employee e : all) {
            String fullName = e.getLastName() + " " + e.getFirstName();
            if (fullName.equalsIgnoreCase(comboValue)) return e;
        }
        return null;
    }

    private void onDeleteEmployeeSchedule(String empName) {
        Employee employee = findEmployeeByString(empName);
        if (employee == null) return;

        // Γρήγορο delete (μπορεί να μπει σε task αν αργεί)
        List<Schedule> toDelete = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getEmployee().getId().equals(employee.getId()))
                .filter(s -> !s.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
        for(Schedule s : toDelete) scheduleService.deleteSchedule(s);

        // Μετά το delete, πρέπει να ξαναφορτώσουμε τη βάση
        loadDataFromDB();

        Alert a = new Alert(Alert.AlertType.INFORMATION, "Future schedules cleared for " + empName);
        a.show();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}