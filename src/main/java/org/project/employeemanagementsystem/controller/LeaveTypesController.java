package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.LeaveType;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.LeaveTypeService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
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
    @FXML private TextField searchField;
    @FXML private Button leaveTypesAddBtn;
    @FXML private Button leaveTypesRemoveBtn;
    @FXML private VBox loadingOverlay;

    private final ObservableList<LeaveType> masterData = FXCollections.observableArrayList();
    private FilteredList<LeaveType> filteredData;
    private LeaveType selectedLeaveType = null;

    // Admin OR HR
    private boolean isPrivileged = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSecurity();
        setupTable();
        setupSearch();
        loadLeaveTypes();
    }

    private void setupSecurity() {
        User currentUser = userSession.getCurrentUser();

        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            isPrivileged =
                    "Admin".equalsIgnoreCase(roleName) ||
                            "HR".equalsIgnoreCase(roleName);
        }

        leaveTypesAddBtn.setDisable(!isPrivileged);
        leaveTypesNameField.setDisable(!isPrivileged);
        leaveTypesMaxDaysField.setDisable(!isPrivileged);
    }

    private void setupTable() {
        leaveTypesID.setCellValueFactory(new PropertyValueFactory<>("id"));
        leaveTypesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        leaveTypesMaxDays.setCellValueFactory(new PropertyValueFactory<>("maxDays"));

        leaveTypesTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null && isPrivileged) {
                        selectedLeaveType = newSel;
                        leaveTypesNameField.setText(newSel.getName());
                        leaveTypesMaxDaysField.setText(String.valueOf(newSel.getMaxDays()));
                        leaveTypesAddBtn.setText("Update Type");
                    } else {
                        clearForm();
                    }
                });

        leaveTypesRemoveBtn.disableProperty().bind(
                leaveTypesTable.getSelectionModel().selectedItemProperty().isNull()
                        .or(new SimpleBooleanProperty(!isPrivileged))
        );
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);
        leaveTypesTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(type -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return type.getName().toLowerCase().contains(lower);
            });
        });
    }

    @FXML
    private void handleRefresh() {
        loadLeaveTypes();
    }

    private void loadLeaveTypes() {
        showLoading(true);

        Task<List<LeaveType>> task = new Task<>() {
            @Override
            protected List<LeaveType> call() {
                return leaveTypeService.getAllLeaveTypes();
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            showLoading(false);
        });

        task.setOnFailed(e -> showLoading(false));

        new Thread(task).start();
    }

    @FXML
    private void handleAddOrUpdateLeaveType() {
        if (!isPrivileged) return;

        String name = leaveTypesNameField.getText();
        String maxDaysStr = leaveTypesMaxDaysField.getText();

        if (name == null || name.isBlank() || maxDaysStr == null || maxDaysStr.isBlank()) {
            showAlert("Validation Error", "All fields are required.");
            return;
        }

        try {
            int maxDays = Integer.parseInt(maxDaysStr);

            if (selectedLeaveType != null) {
                selectedLeaveType.setName(name);
                selectedLeaveType.setMaxDays(maxDays);
                leaveTypeService.saveLeaveType(selectedLeaveType);
            } else {
                LeaveType newType = new LeaveType();
                newType.setName(name);
                newType.setMaxDays(maxDays);
                leaveTypeService.saveLeaveType(newType);
            }

            loadLeaveTypes();
            clearForm();

        } catch (NumberFormatException e) {
            showAlert("Format Error", "Max days must be a valid number.");
        }
    }

    @FXML
    private void handleRemoveLeaveType() {
        if (!isPrivileged || selectedLeaveType == null) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete leave type: " + selectedLeaveType.getName() + " ?"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            leaveTypeService.deleteLeaveType(selectedLeaveType.getId());
            loadLeaveTypes();
            clearForm();
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        } else {
            FadeTransition fade = new FadeTransition(Duration.seconds(0.4), loadingOverlay);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> loadingOverlay.setVisible(false));
            fade.play();
        }
    }

    private void clearForm() {
        leaveTypesNameField.clear();
        leaveTypesMaxDaysField.clear();
        leaveTypesAddBtn.setText("Save Type");
        selectedLeaveType = null;
        leaveTypesTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
