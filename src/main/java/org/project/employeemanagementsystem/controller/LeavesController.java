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

    // Σύνδεση με το Service
    @Autowired
    private LeaveRequestService leaveRequestService;

    // --- FXML UI Components ---
    // Βεβαιώσου ότι τα fx:id στο SceneBuilder είναι ακριβώς τα ίδια!
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;

    @FXML private ComboBox<LeaveType> typeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;
    @FXML private Label remainingDaysLabel;

    private Employee currentEmployee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // 1. Αυτόματη εύρεση του συνδεδεμένου υπαλλήλου (Admin "a")
            currentEmployee = leaveRequestService.getLoggedInEmployee();

            // 2. Συμπλήρωση των Read-Only πεδίων
            populateEmployeeInfo();

            // 3. Γέμισμα του ComboBox με τους τύπους άδειας
            typeCombo.setItems(FXCollections.observableArrayList(leaveRequestService.getAllLeaveTypes()));

            // Ρύθμιση ώστε να φαίνεται το "Όνομα" του τύπου και όχι το αντικείμενο στη λίστα
            setupComboBoxRendering();

            // 4. Listener: Όταν αλλάζει ο τύπος άδειας, υπολόγισε το υπόλοιπο
            typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateRemainingDays(newVal);
                }
            });

        } catch (Exception e) {
            showAlert("Initialization Error", "Could not load user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateEmployeeInfo() {
        if (firstNameField != null) {
            firstNameField.setText(currentEmployee.getFirstName());
            firstNameField.setEditable(false); // Ο χρήστης δεν πρέπει να το αλλάζει
        }
        if (lastNameField != null) {
            lastNameField.setText(currentEmployee.getLastName());
            lastNameField.setEditable(false);
        }
        if (emailField != null) {
            emailField.setText(currentEmployee.getEmail());
            emailField.setEditable(false);
        }
    }

    private void setupComboBoxRendering() {
        // Τρόπος εμφάνισης στη λίστα
        Callback<ListView<LeaveType>, ListCell<LeaveType>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(LeaveType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        };

        typeCombo.setCellFactory(cellFactory);
        typeCombo.setButtonCell(cellFactory.call(null)); // Τρόπος εμφάνισης όταν επιλεγεί
    }

    private void updateRemainingDays(LeaveType type) {
        if (remainingDaysLabel != null && currentEmployee != null) {
            // Κλήση στο Service για τον υπολογισμό
            int remaining = leaveRequestService.getRemainingDays(currentEmployee, type);

            remainingDaysLabel.setText("Remaining Balance: " + remaining + " days");

            // Αλλαγή χρώματος: Κόκκινο αν τελειώνουν, Πράσινο αν έχει υπόλοιπο
            if (remaining <= 0) {
                remainingDaysLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                remainingDaysLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            // Λήψη τιμών από τη φόρμα
            LeaveType selectedType = typeCombo.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String reason = reasonArea.getText();

            // Βασικοί έλεγχοι UI
            if (selectedType == null || start == null || end == null) {
                showAlert("Validation Error", "Please fill in Leave Type and Dates.");
                return;
            }

            // Δημιουργία αντικειμένου
            LeaveRequest request = new LeaveRequest();
            request.setEmployee(currentEmployee);
            request.setLeaveType(selectedType);
            request.setStartDate(start);
            request.setEndDate(end);
            request.setReason(reason);

            // Αποστολή στο Service (Εκεί γίνονται οι έλεγχοι υπολοίπου & ημερομηνιών)
            leaveRequestService.submitRequest(request);

            // Επιτυχία
            showAlert("Success", "Leave request submitted successfully!");

            // Καθαρισμός και ενημέρωση του υπολοίπου
            handleClear();
            // Ξανα-επιλέγουμε τον τύπο για να δούμε το νέο μειωμένο υπόλοιπο (αν θέλουμε)
            // typeCombo.getSelectionModel().clearSelection();

        } catch (RuntimeException e) {
            // Εδώ πιάνουμε τα μηνύματα "Not enough balance" ή "End date before start date"
            showAlert("Request Failed", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        typeCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        if (reasonArea != null) reasonArea.clear();
        if (remainingDaysLabel != null) remainingDaysLabel.setText("Select a leave type to see balance");
    }

    private void showAlert(String title, String message) {
        Alert.AlertType type = title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}