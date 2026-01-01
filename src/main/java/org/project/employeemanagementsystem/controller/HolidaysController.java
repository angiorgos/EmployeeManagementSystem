package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.Holiday;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.HolidayService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class HolidaysController implements Initializable {

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private UserSession userSession;

    @FXML private TableView<Holiday> holidaysTable;
    @FXML private TableColumn<Holiday, Long> holidaysID;
    @FXML private TableColumn<Holiday, String> holidaysName;
    @FXML private TableColumn<Holiday, LocalDate> holidaysDate;

    @FXML private TextField holidaysNameField;
    @FXML private DatePicker holidaysDatePicker;
    @FXML private Button holidaysRemoveBtn;
    @FXML private Button holidaysAddBtn;

    private Holiday selectedHoliday = null;
    private boolean isAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // --- Determine if the current user is an admin
        User currentUser = userSession.getCurrentUser();
        isAdmin = currentUser != null
                && currentUser.getRole() != null
                && "ROLE_ADMIN".equals(currentUser.getRole().getName());

        // --- Table setup
        holidaysTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        holidaysID.setCellValueFactory(new PropertyValueFactory<>("id"));
        holidaysName.setCellValueFactory(new PropertyValueFactory<>("name"));
        holidaysDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadHolidays();

        // --- Listen for table selection
        holidaysTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedHoliday = newSel;
                holidaysNameField.setText(newSel.getName());
                holidaysDatePicker.setValue(newSel.getDate());
                holidaysAddBtn.setText("Confirm Changes");
            } else {
                clearForm();
            }
        });

        // --- Disable Remove button if nothing selected OR user is not admin
        holidaysRemoveBtn.disableProperty().bind(
                holidaysTable.getSelectionModel().selectedItemProperty().isNull()
                        .or(new SimpleBooleanProperty(!isAdmin))
        );

        // --- Disable Add/Confirm and form fields for non-admins
        holidaysAddBtn.setDisable(!isAdmin);
        holidaysNameField.setDisable(!isAdmin);
        holidaysDatePicker.setDisable(!isAdmin);
    }

    private void loadHolidays() {
        holidaysTable.setItems(FXCollections.observableArrayList(holidayService.getAllHolidays()));
    }

    @FXML
    private void handleAddOrUpdateHoliday() {
        if (!isAdmin) {
            showAccessDenied();
            return;
        }

        String name = holidaysNameField.getText();
        LocalDate date = holidaysDatePicker.getValue();

        if (name == null || name.isBlank() || date == null) {
            showAlert("Validation Error", "Please enter both name and date.");
            return;
        }

        try {
            if (selectedHoliday != null) {
                selectedHoliday.setName(name);
                selectedHoliday.setDate(date);
                holidayService.saveHoliday(selectedHoliday);
            } else {
                Holiday newHoliday = new Holiday();
                newHoliday.setName(name);
                newHoliday.setDate(date);
                holidayService.saveHoliday(newHoliday);
            }

            loadHolidays();
            clearForm();

        } catch (Exception e) {
            showAlert("Error", "Holiday date might already exist.");
        }
    }

    @FXML
    private void handleRemoveHoliday() {
        if (!isAdmin) {
            showAccessDenied();
            return;
        }

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

    private void showAccessDenied() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText(null);
        alert.setContentText("Only administrators can modify holidays.");
        alert.showAndWait();
    }
}