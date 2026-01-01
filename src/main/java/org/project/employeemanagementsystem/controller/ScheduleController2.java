package org.project.employeemanagementsystem.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class ScheduleController2 implements Initializable {

    // ====== FXML ======
    @FXML private GridPane weekGrid;
    @FXML private ComboBox<String> employeeComboBox; // προσωρινά String (UI-only)
    // @FXML private Button submitBtn; // προαιρετικό, δεν χρειάζεται να το κρατάς σαν field
    @FXML private GridPane timePickerGrid;
    @FXML private Button submitBtn;

    private Runnable onScheduleSaved;

    public void setOnScheduleSaved(Runnable onScheduleSaved) {
        this.onScheduleSaved = onScheduleSaved;
    }

    // ====== Week config ======
    private static final DayOfWeek[] DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    // 09:00 -> 21:00
    private final int startHour = 9;
    private final int endHour   = 21;

    // Κρατάμε τα cells για εύκολο update
    private final Map<String, VBox> cellMap = new HashMap<>();

    // Επιλεγμένο κελί
    private VBox selectedCell;
    private DayOfWeek selectedDay;
    private int selectedHour;

    private void setInputsEnabled(boolean enabled) {
        boolean disable = !enabled;

        // time pickers
        for (ComboBox<String> cb : new ComboBox[]{
                monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd,
                friStart, friEnd, satStart, satEnd, sunStart, sunEnd
        }) {
            if (cb != null) cb.setDisable(disable);
        }

        // submit button
        if (submitBtn != null) submitBtn.setDisable(disable);
    }

    private void installEmployeePlaceholder() {
        if (employeeComboBox == null) return;

        employeeComboBox.setEditable(false);

        employeeComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Choose Employee");
                } else {
                    setText(item);
                }
            }
        });
    }

    private void renderWeekGrid() {
        weekGrid.getChildren().clear();
        cellMap.clear();

        int rows = (endHour - startHour) + 1; // πχ 6..21 => 16 rows

        for (int r = 0; r < rows; r++) {
            int hour = startHour + r;

            for (int c = 0; c < 7; c++) {
                DayOfWeek day = DAYS[c];

                VBox cell = createWeekCell(day, hour);
                weekGrid.add(cell, c, r);

                GridPane.setHgrow(cell, Priority.ALWAYS);
                GridPane.setVgrow(cell, Priority.ALWAYS);

                cellMap.put(key(day, hour), cell);
            }
        }
    }

    private VBox createWeekCell(DayOfWeek day, int hour) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("day-cell");
        cell.setMinSize(0, 0);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // (Προαιρετικό) μικρό label ώρας μέσα στο κελί για debug
        // Label debug = new Label(day.name().substring(0,3) + " " + String.format("%02d:00", hour));
        // debug.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        // cell.getChildren().add(debug);

        cell.setOnMouseClicked(e -> selectCell(cell, day, hour));
        return cell;
    }

    private void selectCell(VBox cell, DayOfWeek day, int hour) {
        // remove old selection
        if (selectedCell != null) {
            selectedCell.getStyleClass().remove("selected-day");
        }

        selectedCell = cell;
        selectedDay = day;
        selectedHour = hour;

        selectedCell.getStyleClass().add("selected-day");
    }

    private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    @FXML private ComboBox<String> monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd;


    private ObservableList<String> buildTimes() {
        ObservableList<String> items = FXCollections.observableArrayList();

        items.add(""); // κενή επιλογή (ξε-επιλογή)

        for (LocalTime t = LocalTime.of(9, 0); !t.isAfter(LocalTime.of(21, 0)); t = t.plusMinutes(30)) {
            items.add(t.format(TIME_12H)); // 9:00 AM, 9:30 AM, ...
        }
        return items;
    }

    //Κρυψιμο Null τιμης στο combo box
    private void makeBlankSelectable(ComboBox<String> cb) {
        cb.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });
        cb.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });

        cb.getSelectionModel().select(null); // ξεκινάει κενό
    }
    private final List<Label> previewChips = new ArrayList<>();

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalTime.parse(s, TIME_12H);
    }

    private int toRow(LocalTime t) {
        // μετατρέπει ώρα σε row του grid (startHour..endHour)
        return t.getHour() - startHour;
    }

    private void clearPreview() {
        for (Label chip : previewChips) {
            if (chip.getParent() instanceof VBox v) v.getChildren().remove(chip);
        }
        previewChips.clear();
    }

    private String key(DayOfWeek day, int hour) {
        return day + "_" + hour;
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

    private void refreshPreviewFromPickers() {
        clearPreview();

        if (employeeComboBox == null) return;
        String employee = employeeComboBox.getValue();
        if (employee == null || employee.isBlank()) return;

        applyDayRange(DayOfWeek.MONDAY,    monStart, monEnd, employee);
        applyDayRange(DayOfWeek.TUESDAY,   tueStart, tueEnd, employee);
        applyDayRange(DayOfWeek.WEDNESDAY, wedStart, wedEnd, employee);
        applyDayRange(DayOfWeek.THURSDAY,  thuStart, thuEnd, employee);
        applyDayRange(DayOfWeek.FRIDAY,    friStart, friEnd, employee);
        applyDayRange(DayOfWeek.SATURDAY,  satStart, satEnd, employee);
        applyDayRange(DayOfWeek.SUNDAY,    sunStart, sunEnd, employee);
    }

    private void applyDayRange(DayOfWeek day, ComboBox<String> startCb, ComboBox<String> endCb, String employee) {
        if (startCb == null || endCb == null) return;

        LocalTime start = parseTime(startCb.getValue());
        LocalTime end   = parseTime(endCb.getValue());

        if (start == null || end == null) return;
        if (!end.isAfter(start)) return; // πρέπει end > start

        int startRow = start.getHour() - startHour;
        int endRowExclusive = end.getHour() - startHour;

        // αν το end έχει λεπτά (π.χ. 17:30), τότε να "πιάσει" και την ώρα 17
        if (end.getMinute() > 0) endRowExclusive++;

        startRow = Math.max(0, startRow);
        endRowExclusive = Math.min((endHour - startHour) + 1, endRowExclusive);

        for (int r = startRow; r < endRowExclusive; r++) {
            addChipToCell(day, r, employee);
        }
    }

    private void attachAutoPreview(ComboBox<String> cb) {
        if (cb == null) return;
        cb.valueProperty().addListener((obs, o, n) -> refreshPreviewFromPickers());
    }

    @FXML
    private void onSubmit() {
        // ΠΡΟΣΩΡΙΝΑ: εδώ αργότερα θα κάνεις persist + update του Overview.
        // Τώρα: καθαρίζουμε preview + inputs για να περάσεις στον επόμενο employee.

        clearPreview();

        // clear time pickers
        for (ComboBox<String> cb : new ComboBox[]{
                monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd,
                friStart, friEnd, satStart, satEnd, sunStart, sunEnd
        }) {
            if (cb == null) continue;
            cb.getSelectionModel().clearSelection();
            cb.setValue(""); // επειδή έχεις βάλει "" σαν πρώτη επιλογή
        }

        // clear employee selection
        if (employeeComboBox != null) {
            employeeComboBox.getSelectionModel().clearSelection();
            employeeComboBox.setValue(null);
        }

        // clear selected cell highlight
        if (selectedCell != null) selectedCell.getStyleClass().remove("selected-day");
        selectedCell = null;
        selectedDay = null;

        if (onScheduleSaved != null) {
            onScheduleSaved.run();
        }
    }


    private void enforceEndAfterStart(ComboBox<String> startCb, ComboBox<String> endCb) {
        if (startCb == null || endCb == null) return;

        endCb.valueProperty().addListener((obs, oldV, newV) -> {
            LocalTime start = parseTime(startCb.getValue());
            LocalTime end   = parseTime(newV);

            if (start == null || end == null) return;

            if (!end.isAfter(start)) {
                // revert
                endCb.setValue(oldV);

                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setHeaderText("Invalid time range");
                a.setContentText("End time must be later than Start time.");
                a.showAndWait();
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (weekGrid == null) return;

        // Mock employees για UI
        if (employeeComboBox != null) {
            employeeComboBox.getItems().setAll("Maria Pap.", "Giorgos Kon.", "Dimitris Ar.");
        }

        renderWeekGrid();

        // Αρχικά ΚΛΕΙΔΩΜΕΝΑ
        setInputsEnabled(false);

        // Μόλις επιλεγεί υπάλληλος => ΞΕΚΛΕΙΔΩΝΟΥΜΕ
        employeeComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean ok = newV != null && !newV.isBlank();
            setInputsEnabled(ok);

            if (!ok) {
                clearPreview(); // προαιρετικά: καθάρισε preview όταν ξε-επιλεγεί
            }
        });

        //Γεμισμα ComboBox
        ObservableList<String> times = buildTimes();

        for (ComboBox<String> cb : new ComboBox[]{ monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd }) {
            if (cb == null) continue;
            cb.setItems(times);
            makeBlankSelectable(cb); // κενή επιλογή + σωστό rendering
        }


        for (ComboBox<String> cb : new ComboBox[]{
                monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd,
                friStart, friEnd, satStart, satEnd, sunStart, sunEnd
        }) {
            attachAutoPreview(cb);
        }

        enforceEndAfterStart(monStart, monEnd);
        enforceEndAfterStart(tueStart, tueEnd);
        enforceEndAfterStart(wedStart, wedEnd);
        enforceEndAfterStart(thuStart, thuEnd);
        enforceEndAfterStart(friStart, friEnd);
        enforceEndAfterStart(satStart, satEnd);
        enforceEndAfterStart(sunStart, sunEnd);

        // Mock employees για UI
        if (employeeComboBox != null) {
            employeeComboBox.getItems().setAll("Maria Pap.", "Giorgos Kon.", "Dimitris Ar.");
        }
        installEmployeePlaceholder();
    }


}