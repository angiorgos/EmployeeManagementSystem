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

    // 1. ΣΥΝΔΕΣΗ ΜΕ ΤΟ SERVICE
    @Autowired
    private LeaveRequestService leaveRequestService;

    // FXML Στοιχεία (Πρέπει να έχουν τα ίδια fx:id στο SceneBuilder)
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;

    @FXML private ComboBox<LeaveType> typeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea reasonArea;
    @FXML private Label remainingDaysLabel; // Το Label που δείχνει το υπόλοιπο

    private Employee currentEmployee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // 2. ΦΟΡΤΩΣΗ ΔΕΔΟΜΕΝΩΝ ΚΑΤΑ ΤΗΝ ΕΚΚΙΝΗΣΗ

            // Βρίσκουμε ποιος είναι συνδεδεμένος (μέσω του UserSession που έχει το Service)
            currentEmployee = leaveRequestService.getLoggedInEmployee();

            // Γεμίζουμε τα πεδία που δεν αλλάζουν (Read-Only)
            firstNameField.setText(currentEmployee.getFirstName());
            lastNameField.setText(currentEmployee.getLastName());
            emailField.setText(currentEmployee.getEmail());

            // Κλειδώνουμε τα πεδία για να μην τα πειράξει ο χρήστης
            firstNameField.setEditable(false);
            lastNameField.setEditable(false);
            emailField.setEditable(false);

            // Γεμίζουμε το ComboBox με τους τύπους άδειας
            typeCombo.setItems(FXCollections.observableArrayList(leaveRequestService.getAllLeaveTypes()));

            // Ρύθμιση για να φαίνονται τα ονόματα σωστά στο ComboBox
            setupComboBoxRenderer();

            // 3. LISTENER: ΥΠΟΛΟΓΙΣΜΟΣ ΥΠΟΛΟΙΠΟΥ ΣΕ ΠΡΑΓΜΑΤΙΚΟ ΧΡΟΝΟ
            // Μόλις ο χρήστης διαλέξει τύπο άδειας, τρέχει αυτός ο κώδικας
            typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    int remaining = leaveRequestService.getRemainingDays(currentEmployee, newVal);

                    remainingDaysLabel.setText("Remaining Days: " + remaining);

                    // Αλλαγή χρώματος: Κόκκινο αν δεν έχει μέρες, Πράσινο αν έχει
                    if (remaining <= 0) {
                        remainingDaysLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        remainingDaysLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }
            });

        } catch (Exception e) {
            showAlert("Error", "Could not load user data: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            LeaveType type = typeCombo.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String reason = reasonArea.getText();

            // Βασικός έλεγχος ότι συμπλήρωσε τα πάντα
            if (type == null || start == null || end == null) {
                showAlert("Validation Error", "Please fill all required fields.");
                return;
            }

            // Δημιουργία του αντικειμένου
            LeaveRequest request = new LeaveRequest();
            request.setEmployee(currentEmployee);
            request.setLeaveType(type);
            request.setStartDate(start);
            request.setEndDate(end);
            request.setReason(reason);

            // Αποστολή στο Service (Εδώ γίνονται οι έλεγχοι για Σ/Κ και υπόλοιπο)
            leaveRequestService.submitRequest(request);

            showAlert("Success", "Request submitted successfully!");
            handleClear(); // Καθαρισμός φόρμας

        } catch (RuntimeException e) {
            // Πιάνουμε τα μηνύματα λάθους του Service (π.χ. "Not enough days")
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        typeCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        reasonArea.clear();
        remainingDaysLabel.setText("Select a type...");
    }

    // Βοηθητική μέθοδος για να δείχνει το όνομα στο ComboBox αντί για memory address
    private void setupComboBoxRenderer() {
        Callback<ListView<LeaveType>, ListCell<LeaveType>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(LeaveType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        };
        typeCombo.setCellFactory(cellFactory);
        typeCombo.setButtonCell(cellFactory.call(null));
    }

    private void showAlert(String title, String content) {
        Alert.AlertType type = title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}