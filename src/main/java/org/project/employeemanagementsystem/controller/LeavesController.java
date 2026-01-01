package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class LeavesController implements Initializable {

    // ===== SERVICE =====
    @Autowired
    private LeaveRequestService leaveRequestService;

    // ===== FXML FIELDS =====
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private ComboBox<LeaveType> typeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;
    @FXML private Label remainingDaysLabel;

    private Employee currentEmployee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // ===== LOAD LOGGED-IN EMPLOYEE =====
            currentEmployee = leaveRequestService.getLoggedInEmployee();

            // ===== AUTO-FILL EMPLOYEE INFO =====
            firstNameField.setText(currentEmployee.getFirstName());
            lastNameField.setText(currentEmployee.getLastName());
            emailField.setText(currentEmployee.getEmail());
            phoneField.setText(currentEmployee.getPhone());

            // ===== LOCK FIELDS =====
            firstNameField.setEditable(false);
            lastNameField.setEditable(false);
            emailField.setEditable(false);
            phoneField.setEditable(false);

            // ===== LEAVE TYPES =====
            typeCombo.setItems(
                    FXCollections.observableArrayList(
                            leaveRequestService.getAllLeaveTypes()
                    )
            );
            setupComboBoxRenderer();

            // ===== REMAINING DAYS LISTENER =====
            typeCombo.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            int remaining =
                                    leaveRequestService.getRemainingDays(
                                            currentEmployee, newVal
                                    );

                            remainingDaysLabel.setText(
                                    "Remaining Days: " + remaining
                            );

                            if (remaining <= 0) {
                                remainingDaysLabel.setStyle(
                                        "-fx-text-fill: red; -fx-font-weight: bold;"
                                );
                            } else {
                                remainingDaysLabel.setStyle(
                                        "-fx-text-fill: green; -fx-font-weight: bold;"
                                );
                            }
                        }
                    }
            );

        } catch (Exception e) {
            showAlert("Error", "Could not load user data: " + e.getMessage());
        }
    }

    // ===== SUBMIT REQUEST =====
    @FXML
    private void handleSubmit() {
        try {
            LeaveType type = typeCombo.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String reason = reasonArea.getText();

            if (type == null || start == null || end == null) {
                showAlert("Validation Error", "Please fill all required fields.");
                return;
            }

            LeaveRequest request = new LeaveRequest();
            request.setEmployee(currentEmployee);
            request.setLeaveType(type);
            request.setStartDate(start);
            request.setEndDate(end);
            request.setReason(reason);

            leaveRequestService.submitRequest(request);

            showAlert("Success", "Request submitted successfully!");
            handleClear();

        } catch (RuntimeException e) {
            showAlert("Error", e.getMessage());
        } finally {
            //I caught Br Br Patapim
        }
    }

    // ===== CLEAR FORM =====
    @FXML
    private void handleClear() {
        typeCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        reasonArea.clear();
        remainingDaysLabel.setText("Select a type...");
    }

    // ===== COMBOBOX RENDERER =====
    private void setupComboBoxRenderer() {
        Callback<ListView<LeaveType>, ListCell<LeaveType>> cellFactory =
                lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(
                            LeaveType item, boolean empty
                    ) {
                        super.updateItem(item, empty);
                        setText(
                                empty || item == null
                                        ? ""
                                        : item.getName()
                        );
                    }
                };

        typeCombo.setCellFactory(cellFactory);
        typeCombo.setButtonCell(cellFactory.call(null));
    }

    // ===== ALERT HELPER =====
    private void showAlert(String title, String content) {
        Alert.AlertType alertType =
                title.equals("Success")
                        ? Alert.AlertType.INFORMATION
                        : Alert.AlertType.ERROR;

        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
