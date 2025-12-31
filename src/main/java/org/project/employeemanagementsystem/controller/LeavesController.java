package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.service.LeaveTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class LeavesController implements Initializable {

    @Autowired
    private LeaveTypeService leaveTypeService;

    @FXML private ComboBox<LeaveType> typeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Populate leave types
        typeCombo.setItems(
                FXCollections.observableArrayList(
                        leaveTypeService.getAllLeaveTypes()
                )
        );

        // Optional: display readable names in ComboBox
        typeCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(LeaveType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        typeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LeaveType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    @FXML
    private void handleSubmit() {

        LeaveType type = typeCombo.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (type == null || start == null || end == null) {
            showAlert("Validation Error", "Please fill all required fields.");
            return;
        }

        if (end.isBefore(start)) {
            showAlert("Validation Error", "End date cannot be before start date.");
            return;
        }

        // 🔜 Save LeaveRequest here (later)
        System.out.println("Leave Request:");
        System.out.println("Type: " + type.getName());
        System.out.println("From: " + start);
        System.out.println("To: " + end);

        handleClear();
    }

    @FXML
    private void handleClear() {
        typeCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        if (reasonArea != null) reasonArea.clear(); // clears the TextArea
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
