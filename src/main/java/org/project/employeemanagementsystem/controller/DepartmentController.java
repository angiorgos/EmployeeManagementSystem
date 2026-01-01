package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
//xx
@Controller
public class DepartmentController implements Initializable {
    //ΣΥΝΔΕΣΗ ΜΕ ΤΟ SERVICE. ΣΟΥ ΕΒΑΛΑ ΚΑΙ ΜΙΑ ΛΙΣΤΑ. ΑΝ ΘΕΣ ΑΛΛΑΞΕ ΤΗ
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private EmployeeService employeeService;



  //  @FXML private TableView<Department> departmentTable;

    // TableView and columns
    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, Long> idCol;
    @FXML private TableColumn<Department, String> nameCol;
    @FXML private TableColumn<Department, Integer> employeeCountCol;



    // Input fields
    @FXML private TextField DepartmentNameTextArea;
    @FXML private TextArea DepartmentDescriptionTextArea;

    // Buttons
    @FXML private Button CreateDepartmentButton;
    @FXML private Button EditDepartmentButton;
    @FXML private Button DeleteDepartMentButton;

    private ObservableList<Department> departmentList = FXCollections.observableArrayList();

    private Department selectedDepartmen ; // hold the selected department from the table

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Map entity fields to table columns
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));




        //selecting the department from the table
        departmentTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedDepartmen = newSelection;

                        // Access the ID of the selected row
                        Long selectedId = newSelection.getId();
                        System.out.println("Selected Department ID: " + selectedId);

                        // Optional: populate fields
                        DepartmentNameTextArea.setText(newSelection.getName());
                        DepartmentDescriptionTextArea.setText(newSelection.getDescription());
                    }
                });

        departmentList.setAll(departmentService.getAllDepartments());
        populateEmployeeCountColumn();
        departmentTable.setItems(departmentList);
        populateEmployeeCountColumn();
        departmentTable.refresh();


        //setting the text areas and filed with selected department description and name
        departmentTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedDepartmen = newSelection;

                        // Populate fields
                        DepartmentNameTextArea.setText(newSelection.getName());
                        DepartmentDescriptionTextArea.setText(newSelection.getDescription());
                    }
                });


    }


    // creating a new department method
    @FXML
    private void onCreateDepartment() {

        Dialog<Department> dialog = new Dialog<>();
        dialog.setTitle("Create Department");
        dialog.setHeaderText("Enter department details");

        ButtonType createButtonType =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Department Name");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(3);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Disable Create button if name is empty
        Button createButton =
                (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        nameField.textProperty().addListener((obs, oldVal, newVal) ->
                createButton.setDisable(newVal.trim().isEmpty())
        );

        // Convert result
        dialog.setResultConverter(button -> {
            if (button == createButtonType) {
                Department department = new Department();
                department.setName(nameField.getText().trim());
                department.setDescription(descriptionArea.getText());
                return department;
            }
            return null;
        });

        // Save + update table
        dialog.showAndWait().ifPresent(department -> {
            try {
                departmentService.saveDepartment(department);
                departmentList.add(department);
                departmentTable.getSelectionModel().clearSelection();
            } catch (RuntimeException e) {
                showAlert("Error", e.getMessage());
            }
        });
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    //delete the selected department
    @FXML
    private void onDeleteDepartment() {
        Department selected = departmentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a department to delete.");
            return;
        }

        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Department");
        confirmation.setHeaderText("Are you sure you want to delete this department?");
        confirmation.setContentText("Department: " + selected.getName());

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Delete from database
                    departmentService.deleteDepartment(selected);

                    // Remove from TableView
                    departmentList.remove(selected);

                    // Clear selection in TableView
                    departmentTable.getSelectionModel().clearSelection();

                    // Clear the TextField and TextArea
                    DepartmentNameTextArea.clear();
                    DepartmentDescriptionTextArea.clear();

                } catch (Exception e) {
                    showAlert("Error", "Could not delete department: " + e.getMessage());
                }
            }
        });
    }


    //edditing department name and description
    @FXML
    private void onEditDepartment() {
        Department selected = departmentTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No Selection", "Please select a department to edit.");
            return;
        }

        String newName = DepartmentNameTextArea.getText().trim();
        String newDescription = DepartmentDescriptionTextArea.getText();

        // Validation
        if (newName.isEmpty()) {
            showAlert("Validation Error", "Department name cannot be empty.");
            return;
        }

        // Update the department object
        selected.setName(newName);
        selected.setDescription(newDescription);

        try {
            // Save changes to database
            departmentService.saveDepartment(selected);

            // Refresh the table (optional if using ObservableList)
            departmentTable.refresh();

            // Clear selection and fields
            departmentTable.getSelectionModel().clearSelection();
            DepartmentNameTextArea.clear();
            DepartmentDescriptionTextArea.clear();

        } catch (RuntimeException e) {
            showAlert("Error", e.getMessage());
        }
    }


    //3rd row number of employess
    private void populateEmployeeCountColumn() {
        // Use a map of DepartmentId -> Employee Count
        Map<Long, Long> employeeCounts = employeeService.getAllEmployees()
                .stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().getId(),
                        Collectors.counting()
                ));

        // Set cell value factory for the TableColumn
        employeeCountCol.setCellValueFactory(cellData -> {
            Department dept = cellData.getValue();
            long count = employeeCounts.getOrDefault(dept.getId(), 0L);
            return new SimpleIntegerProperty((int) count).asObject();
        });
    }




}