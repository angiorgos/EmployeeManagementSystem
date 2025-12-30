package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;

@Controller
public class DepartmentController implements Initializable {

    @Autowired
    private DepartmentService departmentService;

    // ---------------- Table ----------------
    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, Long> idCol;
    @FXML private TableColumn<Department, String> nameCol;
    @FXML private TableColumn<Department, Integer> numberCol;

    // ---------------- Form ----------------
    @FXML private TextField departmentNameField;
    @FXML private TextArea departmentDescriptionField;

    // ---------------- Buttons ----------------
    @FXML private Button addOrUpdateButton;
    @FXML private Button removeButton;

    private Department selectedDepartment = null;

    @Override
    public void initialize(URL location, java.util.ResourceBundle resources) {
        // 1️⃣ Bind columns
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getId()));
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));
        numberCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(
                cell.getValue().getEmployees() == null ? 0 : cell.getValue().getEmployees().size()
        ).asObject());

        // 2️⃣ Load table
        loadDepartments();

        // 3️⃣ Table selection listener
        departmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedDepartment = newSel;
                departmentNameField.setText(selectedDepartment.getName());
                departmentDescriptionField.setText(selectedDepartment.getDescription());
                addOrUpdateButton.setText("Confirm Changes");
            } else {
                clearForm();
            }
        });

        // 4️⃣ Disable remove button if nothing selected
        removeButton.disableProperty().bind(departmentTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        departmentTable.setItems(FXCollections.observableArrayList(departments));
    }

    @FXML
    private void handleAddOrUpdateDepartment() {
        String name = departmentNameField.getText();
        String description = departmentDescriptionField.getText();

        if (name == null || name.isBlank()) {
            showAlert("Validation Error", "Department name cannot be empty.");
            return;
        }

        try {
            if (selectedDepartment != null) {
                // Update
                selectedDepartment.setName(name);
                selectedDepartment.setDescription(description);
                departmentService.saveDepartment(selectedDepartment);
            } else {
                // Add new
                Department dept = new Department();
                dept.setName(name);
                dept.setDescription(description);
                departmentService.saveDepartment(dept);
            }

            loadDepartments();
            clearForm();
        } catch (Exception e) {
            showAlert("Error", "Could not save department. It might already exist.");
        }
    }

    @FXML
    private void handleRemoveDepartment() {
        if (selectedDepartment != null) {
            try {
                departmentService.deleteDepartment(selectedDepartment);
                loadDepartments();
                clearForm();
            } catch (Exception e) {
                showAlert("Error", "Could not delete department.");
            }
        }
    }

    private void clearForm() {
        departmentNameField.clear();
        departmentDescriptionField.clear();
        addOrUpdateButton.setText("Add");
        departmentTable.getSelectionModel().clearSelection();
        selectedDepartment = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
