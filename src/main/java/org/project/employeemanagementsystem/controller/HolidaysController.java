package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.Holiday;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.HolidayService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class HolidaysController implements Initializable {

    @Autowired private HolidayService holidayService;
    @Autowired private UserSession userSession;

    @FXML private TableView<Holiday> holidaysTable;
    @FXML private TableColumn<Holiday, Long> holidaysID;
    @FXML private TableColumn<Holiday, String> holidaysName;
    @FXML private TableColumn<Holiday, LocalDate> holidaysDate;

    @FXML private TextField holidaysNameField;
    @FXML private DatePicker holidaysDatePicker;
    @FXML private TextField searchField; // Προσθήκη search
    @FXML private Button holidaysRemoveBtn;
    @FXML private Button holidaysAddBtn;
    @FXML private VBox loadingOverlay; // Προσθήκη loading

    private final ObservableList<Holiday> masterData = FXCollections.observableArrayList();
    private FilteredList<Holiday> filteredData;
    private Holiday selectedHoliday = null;
    private boolean isAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSecurity();
        setupTable();
        setupSearch();
        loadHolidays();
    }

    private void setupSecurity() {
        User currentUser = userSession.getCurrentUser();
        isAdmin = currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equals(currentUser.getRole().getName());

        holidaysAddBtn.setDisable(!isAdmin);
        holidaysNameField.setDisable(!isAdmin);
        holidaysDatePicker.setDisable(!isAdmin);
    }

    private void setupTable() {
        holidaysID.setCellValueFactory(new PropertyValueFactory<>("id"));
        holidaysName.setCellValueFactory(new PropertyValueFactory<>("name"));
        holidaysDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        holidaysTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedHoliday = newSel;
                holidaysNameField.setText(newSel.getName());
                holidaysDatePicker.setValue(newSel.getDate());
                holidaysAddBtn.setText("Update Holiday");
            } else {
                clearForm();
            }
        });

        holidaysRemoveBtn.disableProperty().bind(holidaysTable.getSelectionModel().selectedItemProperty().isNull().or(new SimpleBooleanProperty(!isAdmin)));
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);
        holidaysTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(h -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return h.getName().toLowerCase().contains(lower) || h.getDate().toString().contains(lower);
            });
        });
    }

    @FXML
    private void handleRefresh() { loadHolidays(); }

    private void loadHolidays() {
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        Task<List<Holiday>> task = new Task<>() {
            @Override protected List<Holiday> call() { return holidayService.getAllHolidays(); }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void handleAddOrUpdateHoliday() {
        if (!isAdmin) return;

        String name = holidaysNameField.getText();
        LocalDate date = holidaysDatePicker.getValue();

        if (name == null || name.isBlank() || date == null) {
            showAlert("Validation Error", "Please fill in all required fields.");
            return;
        }

        if (selectedHoliday != null) {
            selectedHoliday.setName(name);
            selectedHoliday.setDate(date);
            holidayService.saveHoliday(selectedHoliday);
        } else {
            Holiday h = new Holiday();
            h.setName(name);
            h.setDate(date);
            holidayService.saveHoliday(h);
        }

        loadHolidays();
        clearForm();
    }

    @FXML
    private void handleRemoveHoliday() {
        if (!isAdmin || selectedHoliday == null) return;

        holidayService.deleteHoliday(selectedHoliday.getId());
        loadHolidays();
        clearForm();
    }

    private void clearForm() {
        holidaysNameField.clear();
        holidaysDatePicker.setValue(null);
        holidaysAddBtn.setText("Save Holiday");
        selectedHoliday = null;
        holidaysTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setContentText(content);
        a.showAndWait();
    }
}