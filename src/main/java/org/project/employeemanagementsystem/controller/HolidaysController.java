package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.Holiday;
import org.project.employeemanagementsystem.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.project.employeemanagementsystem.service.HolidayService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class HolidaysController implements Initializable {

    @Autowired
    private HolidayService holidayService;

    @FXML private TableView<Holiday> holidaysTable;
    @FXML private TableColumn<Holiday, Long> holidaysID;
    @FXML private TableColumn<Holiday, String> holidaysName;
    @FXML private TableColumn<Holiday, LocalDate> holidaysDate;

    @FXML private TextField holidaysNameField;
    @FXML private DatePicker holidaysDatePicker;
    @FXML private Button holidaysRemoveBtn;
    @FXML private Button holidaysAddBtn;

    private Holiday selectedHoliday = null; // Holds the currently selected holiday

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        holidaysTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        holidaysID.setCellValueFactory(new PropertyValueFactory<>("id"));
        holidaysName.setCellValueFactory(new PropertyValueFactory<>("name"));
        holidaysDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadHolidays();

        // 1️⃣ Listen for selection in the table
        holidaysTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedHoliday = newSel;
                holidaysNameField.setText(selectedHoliday.getName());
                holidaysDatePicker.setValue(selectedHoliday.getDate());
                holidaysAddBtn.setText("Confirm Changes");
            } else {
                // No selection, reset
                selectedHoliday = null;
                holidaysNameField.clear();
                holidaysDatePicker.setValue(null);
                holidaysAddBtn.setText("Add");
            }
        });

        // 2️⃣ Remove button disables if nothing is selected
        holidaysRemoveBtn.disableProperty().bind(holidaysTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadHolidays() {
        holidaysTable.setItems(FXCollections.observableArrayList(holidayService.getAllHolidays()));
    }

    @FXML
    private void handleAddOrUpdateHoliday() {
        String name = holidaysNameField.getText();
        LocalDate date = holidaysDatePicker.getValue();

        if (name == null || name.isBlank() || date == null) {
            showAlert("Validation Error", "Please enter both name and date.");
            return;
        }

        try {
            if (selectedHoliday != null) {
                // ✅ Update existing holiday
                selectedHoliday.setName(name);
                selectedHoliday.setDate(date);
                holidayService.saveHoliday(selectedHoliday);
            } else {
                // ✅ Add new holiday
                Holiday newHoliday = new Holiday();
                newHoliday.setName(name);
                newHoliday.setDate(date);
                holidayService.saveHoliday(newHoliday);
            }

            // Refresh table and reset form
            loadHolidays();
            clearForm();

        } catch (Exception e) {
            showAlert("Error", "Holiday date might already exist.");
        }
    }

    @FXML
    private void handleRemoveHoliday() {
        if (selectedHoliday != null) {
            try {
                holidayService.deleteHoliday(selectedHoliday.getId());
                loadHolidays();
                clearForm();
            } catch (Exception e) {
                showAlert("Error", "Could not delete holiday.");
            }
        }
    }

    private void clearForm() {
        holidaysNameField.clear();
        holidaysDatePicker.setValue(null);
        holidaysAddBtn.setText("Add");
        holidaysTable.getSelectionModel().clearSelection();
        selectedHoliday = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

