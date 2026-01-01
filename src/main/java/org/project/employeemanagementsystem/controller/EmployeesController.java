package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class EmployeesController implements Initializable {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeesController(EmployeeService employeeService, DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    // --- CONTAINERS ---
    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;

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
        loadData();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }


    private void showTable() {
        formViewContainer.setVisible(false);
        tableViewContainer.setVisible(true);
        loadData();
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

        // ALERT ME STYLE
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Employee saved successfully!");
        styleDialog(alert); // <--- ΕΔΩ ΕΦΑΡΜΟΖΟΥΜΕ ΤΟ THEME
        alert.showAndWait();
    }

    @FXML
    public void handleRefresh() {
        loadData();
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

                    // DELETE CONFIRMATION ALERT (STYLED)
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Employee");
                    confirm.setHeaderText("Delete " + emp.getLastName() + "?");
                    confirm.setContentText("Are you sure? This action cannot be undone.");
                    styleDialog(confirm);

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        employeeService.deleteEmployee(emp.getId());
                        loadData();

                        // DELETE SUCCESS ALERT (STYLED)
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Deleted");
                        info.setHeaderText(null);
                        info.setContentText("Employee has been deleted.");
                        styleDialog(info);
                        info.showAndWait();
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
                employeeTable.refresh();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Salary Updated");
                alert.setHeaderText(null);
                alert.setContentText("Salary updated successfully!");
                styleDialog(alert);
                alert.showAndWait();

            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText(null);
                alert.setContentText("Please enter a valid number!");
                styleDialog(alert);
                alert.showAndWait();
            }
        });
    }

    // --- ΒΟΗΘΗΤΙΚΗ ΜΕΘΟΔΟΣ ΓΙΑ ΣΥΝΔΕΣΗ CSS ---
    private void styleDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        try {
            // Συνδέουμε το CSS αρχείο στο DialogPane
            dialogPane.getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
            dialogPane.getStyleClass().add("my-dialog");
        } catch (Exception e) {
            System.out.println("Could not load theme.css for dialog.");
        }
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

    private void loadData() {
        filteredData = new FilteredList<>(FXCollections.observableArrayList(employeeService.getAllEmployees()));
        employeeTable.setItems(filteredData);
        applyFilter(searchField.getText());
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
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText("Please fill required fields (Name, Email)");
            styleDialog(alert);
            alert.showAndWait();
            return false;
        }
        return true;
    }
}