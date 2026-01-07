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
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.ScheduleService;
import org.project.employeemanagementsystem.util.UserSession;
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
    @Autowired private UserSession userSession; // <-- Added for role-based tab

    //FXML ELEMENTS
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

    //STATE
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private LocalDate selectedDate = LocalDate.now();
    private String currentViewMode = "MONTH";

    private List<Schedule> allSchedulesInMemory = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup view toggle buttons
        if (monthBtn != null) {
            monthBtn.setOnAction(e -> {
                currentViewMode = "MONTH";
                refreshLocalView();
            });
            monthBtn.setSelected(true);
        }

        if (weekBtn != null) {
            weekBtn.setOnAction(e -> {
                currentViewMode = "WEEK";
                currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                refreshLocalView();
            });
        }

        if (newScheduleViewController != null) {
            newScheduleViewController.setOnScheduleSaved(this::onNewScheduleSaved);
            newScheduleViewController.setOnScheduleDeleted(this::onDeleteEmployeeSchedule);
            newScheduleViewController.setEmployeeService(employeeService);
        }

        // Apply role-based visibility for "New Schedule" tab
        applyRoleTabVisibility();

        // Load schedules from DB initially
        loadDataFromDB();
    }

    // ===================== ROLE-BASED TAB VISIBILITY =====================
    private void applyRoleTabVisibility() {
        User user = userSession.getCurrentUser();
        String role = user != null && user.getRole() != null ? user.getRole().getName() : "";

        boolean isAdmin = "Admin".equalsIgnoreCase(role);
        boolean isHR = "HR".equalsIgnoreCase(role);

        // New Schedule tab is second tab (index 1)
        if (scheduleTabs != null && scheduleTabs.getTabs().size() > 1) {
            Tab newScheduleTab = scheduleTabs.getTabs().get(1);
            if (!(isAdmin || isHR)) {
                scheduleTabs.getTabs().remove(newScheduleTab); // completely remove for non-admin/HR
            }
        }
    }

    // ===================== DATA LOADING =====================
    private void loadDataFromDB() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        }

        PauseTransition uiRenderDelay = new PauseTransition(Duration.millis(50));
        uiRenderDelay.setOnFinished(ev -> {
            Task<List<Schedule>> task = new Task<>() {
                @Override
                protected List<Schedule> call() {
                    return scheduleService.getAllSchedules();
                }
            };

            task.setOnSucceeded(e -> {
                allSchedulesInMemory = task.getValue();
                refreshLocalView();
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

    // ===================== LOCAL VIEW REFRESH =====================
    private void refreshLocalView() {
        if ("MONTH".equals(currentViewMode)) renderMonthGrid();
        else renderWeekGrid();
        updateRightPanel(selectedDate);
    }

    @FXML private void onNextMonth() {
        if ("MONTH".equals(currentViewMode)) currentMonth = currentMonth.plusMonths(1);
        else currentWeekStart = currentWeekStart.plusWeeks(1);
        refreshLocalView();
    }

    @FXML private void onPrevMonth() {
        if ("MONTH".equals(currentViewMode)) currentMonth = currentMonth.minusMonths(1);
        else currentWeekStart = currentWeekStart.minusWeeks(1);
        refreshLocalView();
    }

    @FXML private void onToday() {
        selectedDate = LocalDate.now();
        currentMonth = YearMonth.now();
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if(todayBtn != null) todayBtn.setSelected(false);
        refreshLocalView();
    }

    // ===================== GRID RENDERING =====================
    private void renderMonthGrid() {
        monthView.getChildren().clear();
        monthLabel.setText(capitalize(currentMonth.getMonth().toString()) + " " + currentMonth.getYear());

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate gridStart = firstOfMonth.minusDays(shift);

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
        root.setMinSize(0,0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        boolean inMonth = date.getMonth().equals(visibleMonth);

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

        VBox employeesBox = new VBox(2);
        employeesBox.setPadding(new javafx.geometry.Insets(2));
        if (dailySchedules != null) {
            for (Schedule s : dailySchedules) {
                employeesBox.getChildren().add(employeeChip(s.getEmployee().getLastName(), s.getStartTime(), s.getEndTime()));
            }
        }

        ScrollPane scrollPane = new ScrollPane(employeesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("cell-scroll-pane");

        root.getChildren().addAll(header, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        if (!inMonth && "MONTH".equals(currentViewMode)) root.getStyleClass().add("out-month");
        if (date.equals(selectedDate)) root.getStyleClass().add("selected-day");

        root.setOnMouseClicked(e -> {
            selectedDate = date;
            updateRightPanel(date);
            refreshLocalView();
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
            List<Schedule> daily = allSchedulesInMemory.stream()
                    .filter(s -> s.getDate().equals(date))
                    .collect(Collectors.toList());

            if (daily.isEmpty()) workingEmployeesList.getItems().setAll("No schedules");
            else {
                List<String> info = daily.stream()
                        .map(s -> s.getEmployee().getFirstName() + " " + s.getEmployee().getLastName() +
                                "\n" + s.getStartTime() + " - " + s.getEndTime())
                        .collect(Collectors.toList());
                workingEmployeesList.getItems().setAll(info);
            }
        }
    }

    // ===================== SAVE / DELETE =====================
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
                    Employee employee = findEmployeeByString(payload.employee);
                    if (employee == null) throw new Exception("Employee not found");

                    LocalDate from = payload.validFrom;
                    LocalDate to = payload.validTo;

                    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                        LocalDate currentDate = d;

                        List<Schedule> existing = scheduleService.getAllSchedules().stream()
                                .filter(s -> s.getDate().equals(currentDate) && s.getEmployee().getId().equals(employee.getId()))
                                .collect(Collectors.toList());
                        for (Schedule s : existing) scheduleService.deleteSchedule(s);

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

            saveTask.setOnSucceeded(e -> reloadCacheInBackground());
            saveTask.setOnFailed(e -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save: " + saveTask.getException().getMessage());
                alert.show();
            });

            new Thread(saveTask).start();
        });
        uiRenderDelay.play();
    }

    private void reloadCacheInBackground() {
        Task<List<Schedule>> reloadTask = new Task<>() {
            @Override protected List<Schedule> call() {
                return scheduleService.getAllSchedules();
            }
        };

        reloadTask.setOnSucceeded(evt -> {
            allSchedulesInMemory = reloadTask.getValue();
            refreshLocalView();
            if(scheduleTabs != null) scheduleTabs.getSelectionModel().select(0);
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

        List<Schedule> toDelete = scheduleService.getAllSchedules().stream()
                .filter(s -> s.getEmployee().getId().equals(employee.getId()))
                .filter(s -> !s.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
        for(Schedule s : toDelete) scheduleService.deleteSchedule(s);

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
