package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.LeaveTypeService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class LeaveTypesController implements Initializable {

    @Autowired
    private LeaveTypeService leaveTypeService;

    @Autowired
    private UserSession userSession;

    @FXML private TableView<LeaveType> leaveTypesTable;
    @FXML private TableColumn<LeaveType, Long> leaveTypesID;
    @FXML private TableColumn<LeaveType, String> leaveTypesName;
    @FXML private TableColumn<LeaveType, Integer> leaveTypesMaxDays;

    @FXML private TextField leaveTypesNameField;
    @FXML private TextField leaveTypesMaxDaysField;
    @FXML private Button leaveTypesAddBtn;
    @FXML private Button leaveTypesRemoveBtn;

    private LeaveType selectedLeaveType = null;
    private boolean isAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // --- Determine if user is admin
        User currentUser = userSession.getCurrentUser();
        isAdmin = currentUser != null
                && currentUser.getRole() != null
                && "ROLE_ADMIN".equals(currentUser.getRole().getName());

        // --- Table setup
        leaveTypesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        leaveTypesID.setCellValueFactory(new PropertyValueFactory<>("id"));
        leaveTypesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        leaveTypesMaxDays.setCellValueFactory(new PropertyValueFactory<>("maxDays"));

        loadLeaveTypes();

        // --- Selection listener
        leaveTypesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedLeaveType = newSel;
                leaveTypesNameField.setText(newSel.getName());
                leaveTypesMaxDaysField.setText(String.valueOf(newSel.getMaxDays()));
                leaveTypesAddBtn.setText("Confirm Changes");
            } else {
                clearForm();
            }
        });

        // --- Disable Remove button if nothing selected OR user not admin
        leaveTypesRemoveBtn.disableProperty().bind(
                leaveTypesTable.getSelectionModel().selectedItemProperty().isNull()
                        .or(new SimpleBooleanProperty(!isAdmin))
        );

        // --- Disable Add/Confirm button for non-admins
        leaveTypesAddBtn.setDisable(!isAdmin);

        // --- Disable text fields for non-admins
        leaveTypesNameField.setDisable(!isAdmin);
        leaveTypesMaxDaysField.setDisable(!isAdmin);
    }

    private void loadLeaveTypes() {
        leaveTypesTable.setItems(FXCollections.observableArrayList(leaveTypeService.getAllLeaveTypes()));
    }

    @FXML
    private void handleAddOrUpdateLeaveType() {
        if (!isAdmin) return;

        String name = leaveTypesNameField.getText();
        Integer maxDays;

        try {
            maxDays = Integer.parseInt(leaveTypesMaxDaysField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Max days must be a number.");
            return;
        }

        if (name == null || name.isBlank()) {
            showAlert("Validation Error", "Please enter a leave name.");
            return;
        }

        if (selectedLeaveType != null) {
            // --- UPDATE
            selectedLeaveType.setName(name);
            selectedLeaveType.setMaxDays(maxDays);
            leaveTypeService.saveLeaveType(selectedLeaveType);
        } else {
            // --- ADD
            LeaveType newLeaveType = new LeaveType();
            newLeaveType.setName(name);
            newLeaveType.setMaxDays(maxDays);
            leaveTypeService.saveLeaveType(newLeaveType);
        }

        loadLeaveTypes();
        leaveTypesTable.refresh();
        clearForm();
    }

    @FXML
    private void handleRemoveLeaveType() {
        if (!isAdmin || selectedLeaveType == null) return;

        leaveTypeService.deleteLeaveType(selectedLeaveType.getId());
        loadLeaveTypes();
        clearForm();
    }

    private void clearForm() {
        leaveTypesNameField.clear();
        leaveTypesMaxDaysField.clear();
        leaveTypesAddBtn.setText("Add");
        leaveTypesTable.getSelectionModel().clearSelection();
        selectedLeaveType = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
