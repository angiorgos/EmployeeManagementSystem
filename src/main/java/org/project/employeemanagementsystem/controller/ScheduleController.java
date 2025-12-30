package org.project.employeemanagementsystem.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Controller;

@Controller
public class ScheduleController {
    //@Autowired
    //private ScheduleService scheduleService;

    @FXML
    private Label monthLabel;

    @FXML
    private GridPane monthView;

    @FXML
    private Label selectedDayLabel;

    @FXML
    private ListView<String> workingEmployeesList;


    @FXML
    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML
    public void initialize() {
        renderMonth(currentMonth);
        updateRightPanel(selectedDate);
    }

    @FXML
    void onNextMonth(ActionEvent event) {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth(currentMonth);
    }

    @FXML
    void onPrevMonth(ActionEvent event) {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth(currentMonth);
    }

    @FXML
    void onToday(ActionEvent event) {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        renderMonth(currentMonth);
        updateRightPanel(selectedDate);
    }

    private void renderMonth(YearMonth ym) {
        monthView.getChildren().clear();
        monthLabel.setText(capitalize(ym.getMonth().toString()) + " " + ym.getYear());

        LocalDate firstOfMonth = ym.atDay(1);

        // Monday-first: Mon->0 ... Sun->6
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

        // Weekday label (μόνο στην 1η σειρά)
        if (rowIndex == 0) {
            Label weekdayLabel = new Label(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
            weekdayLabel.getStyleClass().add("weekday-label");
            root.getChildren().add(weekdayLabel);
        }

        // Day number
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");

        VBox employeesBox = new VBox(2);

        boolean inMonth = date.getMonth().equals(visibleMonth.getMonth());
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
        selectedDayLabel.setText(date.toString());

        // mock data για τη δεξιά λίστα
        workingEmployeesList.getItems().setAll(
                "Maria P. (09:00 - 17:00)",
                "Giorgos K. (09:00 - 17:00)",
                "Dimitris A. (12:00 - 20:00)"
        );
    }

    private String capitalize(String s) {
        s = s.toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }


}

