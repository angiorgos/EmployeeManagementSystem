package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class EmployeesController implements Initializable {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeesController(EmployeeService employeeService, DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    // --- VIEWS & OVERLAYS ---
    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay; // <--- ΤΟ ΝΕΟ LOADING SCREEN

    // --- TABLE ELEMENTS ---
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, Long> colId;
    @FXML private TableColumn<Employee, String> colFirstName;
    @FXML private TableColumn<Employee, String> colLastName;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, String> colEmail;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, Double> colSalary;
    @FXML private TableColumn<Employee, Void> colActions;

    @FXML private TextField searchField;

    // --- FORM ELEMENTS ---
    @FXML private Label formTitle;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ssnField;
    @FXML private TextField salaryField;
    @FXML private ComboBox<Department> departmentCombo;

    private FilteredList<Employee> filteredData;
    private Employee currentEditingEmployee = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupDepartmentCombo();

        // Ξεκινάμε τη φόρτωση μόλις είναι έτοιμο το UI (όπως στο Dashboard)
        Platform.runLater(this::loadData);

        // Search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    // --- LOADING LOGIC (ΙΔΙΑ ΜΕ DASHBOARD) ---

    private void loadData() {
        // Εμφανίζουμε το Loading Overlay σε περίπτωση που ήταν κρυμμένο (π.χ. στο Refresh)
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() throws Exception {
                // ΒΑΡΙΑ ΔΟΥΛΕΙΑ ΣΤΟ BACKGROUND
                return employeeService.getAllEmployees();
            }
        };

        task.setOnSucceeded(event -> {
            List<Employee> employees = task.getValue();

            // Ενημέρωση του πίνακα
            filteredData = new FilteredList<>(FXCollections.observableArrayList(employees));
            employeeTable.setItems(filteredData);
            applyFilter(searchField.getText());

            // --- ANIMATION: ΑΦΑΙΡΕΣΗ LOADING SCREEN ---
            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
                fadeOut.play();
            });
            delay.play();
        });

        task.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            Throwable error = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load data: " + error.getMessage());
            styleDialog(alert);
            alert.show();
        });

        new Thread(task).start();
    }

    // --- CRUD ACTIONS ---

    @FXML
    public void handleRefresh() {
        loadData(); // Ξανατρέχει το Task και εμφανίζει το overlay
    }

    @FXML
    public void handleSaveEmployee() {
        if (!validateForm()) return;

        if (currentEditingEmployee == null) {
            currentEditingEmployee = new Employee();
        }

        currentEditingEmployee.setFirstName(firstNameField.getText());
        currentEditingEmployee.setLastName(lastNameField.getText());
        currentEditingEmployee.setEmail(emailField.getText());
        currentEditingEmployee.setPhone(phoneField.getText());
        currentEditingEmployee.setSsn(ssnField.getText());
        currentEditingEmployee.setDepartment(departmentCombo.getValue());

        try {
            currentEditingEmployee.setSalary(Double.parseDouble(salaryField.getText()));
        } catch (NumberFormatException e) {
            currentEditingEmployee.setSalary(0.0);
        }

        employeeService.saveEmployee(currentEditingEmployee);

        showTable();

        // Alert Success
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Employee saved successfully!");
        styleDialog(alert);
        alert.showAndWait();

        // Refresh data to show changes
        loadData();
    }

    // --- VIEW SWITCHING ---

    private void showTable() {
        formViewContainer.setVisible(false);
        tableViewContainer.setVisible(true);
    }

    private void showForm(Employee employee) {
        this.currentEditingEmployee = employee;

        if (employee == null) {
            formTitle.setText("New Employee");
            clearForm();
        } else {
            formTitle.setText("Edit Employee");
            firstNameField.setText(employee.getFirstName());
            lastNameField.setText(employee.getLastName());
            emailField.setText(employee.getEmail());
            phoneField.setText(employee.getPhone());
            ssnField.setText(employee.getSsn());
            salaryField.setText(String.valueOf(employee.getSalary() != null ? employee.getSalary() : 0.0));
            departmentCombo.setValue(employee.getDepartment());
        }

        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(true);
    }

    @FXML
    public void handleAddEmployee() {
        showForm(null);
    }

    @FXML
    public void handleBackToTable() {
        showTable();
    }

    // --- TABLE SETUP ---

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        colDepartment.setCellValueFactory(cell -> {
            Employee emp = cell.getValue();
            if (emp.getDepartment() != null) {
                return new javafx.beans.property.SimpleStringProperty(emp.getDepartment().getName());
            } else {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });

        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnSalary = new Button("Salary");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnSalary, btnDelete);

            {
                pane.getStyleClass().add("action-box");
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnSalary.getStyleClass().addAll("table-btn", "table-btn-salary");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");

                btnEdit.setOnAction(event -> showForm(getTableView().getItems().get(getIndex())));
                btnSalary.setOnAction(event -> openSalaryDialog(getTableView().getItems().get(getIndex())));

                btnDelete.setOnAction(event -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Employee");
                    confirm.setHeaderText("Delete " + emp.getLastName() + "?");
                    confirm.setContentText("Are you sure?");
                    styleDialog(confirm);

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        employeeService.deleteEmployee(emp.getId());

                        Alert info = new Alert(Alert.AlertType.INFORMATION, "Employee deleted.");
                        styleDialog(info);
                        info.showAndWait();

                        loadData(); // Refresh via Task
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                    setStyle("-fx-padding: 0;");
                }
            }
        });
    }

    // --- HELPERS ---

    private void openSalaryDialog(Employee employee) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(employee.getSalary() != null ? employee.getSalary() : 0.0));
        dialog.setTitle("Manage Salary");
        dialog.setHeaderText("Update Salary for: " + employee.getLastName());
        dialog.setContentText("New Salary Amount (€):");
        styleDialog(dialog);

        dialog.showAndWait().ifPresent(salaryStr -> {
            try {
                double newSalary = Double.parseDouble(salaryStr);
                employee.setSalary(newSalary);
                employeeService.saveEmployee(employee);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Salary updated successfully!");
                styleDialog(alert);
                alert.showAndWait();

                loadData(); // Refresh via Task
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid number format!");
                styleDialog(alert);
                alert.showAndWait();
            }
        });
    }

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception e) { /* ignore */ }
    }

    private void setupDepartmentCombo() {
        departmentCombo.getItems().setAll(departmentService.getAllDepartments());
        departmentCombo.setConverter(new StringConverter<Department>() {
            @Override
            public String toString(Department d) { return d == null ? "" : d.getName(); }
            @Override
            public Department fromString(String s) { return null; }
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) filteredData.setPredicate(emp -> true);
        else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(emp ->
                    emp.getLastName().toLowerCase().contains(lower) ||
                            emp.getFirstName().toLowerCase().contains(lower)
            );
        }
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        ssnField.clear();
        salaryField.clear();
        departmentCombo.setValue(null);
    }

    private boolean validateForm() {
        if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() || emailField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill required fields (Name, Email)");
            styleDialog(alert);
            alert.showAndWait();
            return false;
        }
        return true;
    }
}