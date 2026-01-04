package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
public class EmployeesController implements Initializable {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeesController(EmployeeService employeeService, DepartmentService departmentService) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
    }

    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay;

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
    @FXML private Label formTitle;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ssnField;
    @FXML private TextField salaryField;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private DatePicker hireDateField;
    @FXML private TextField addressField;

    private final ObservableList<Employee> masterData = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredData;
    private Employee currentEditingEmployee = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupDepartmentCombo();
        setupValidationListeners();

        // ΝΕΟ: Εφαρμογή στυλ για τους ανενεργούς υπαλλήλους (Soft Deleted)
        setupRowStyling();

        filteredData = new FilteredList<>(masterData, p -> true);
        employeeTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        Platform.runLater(this::loadData);
    }

    // --- DATA LOADING ---

    private void loadData() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() throws Exception {
                // Φέρνουμε ΟΛΟΥΣ (και τους ανενεργούς) για να φαίνεται το ιστορικό
                return employeeService.getAllEmployees();
            }
        };

        task.setOnSucceeded(event -> {
            masterData.setAll(task.getValue());
            fadeOutLoading();
        });

        task.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            showErrorAlert("Load Failed", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // --- SAVE ACTION ---

    @FXML
    public void handleSaveEmployee() {
        resetFieldStyles();
        if (!validateForm()) return;

        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        // Safe creation of object to avoid hibernate session issues
        Employee employeeToSave = new Employee();
        if (currentEditingEmployee != null) {
            employeeToSave.setId(currentEditingEmployee.getId());
            // Κρατάμε το υπάρχον exitDate αν υπάρχει
            employeeToSave.setExitDate(currentEditingEmployee.getExitDate());
        }

        employeeToSave.setFirstName(firstNameField.getText());
        employeeToSave.setLastName(lastNameField.getText());
        employeeToSave.setEmail(emailField.getText());
        employeeToSave.setPhone(phoneField.getText());
        employeeToSave.setSsn(ssnField.getText());
        employeeToSave.setDepartment(departmentCombo.getValue());
        employeeToSave.setAddress(addressField.getText());

        // Χειρισμός Hire Date
        if (hireDateField.getValue() != null) {
            employeeToSave.setHireDate(hireDateField.getValue());
        } else {
            employeeToSave.setHireDate(LocalDate.now());
        }

        try {
            employeeToSave.setSalary(
                    salaryField.getText() == null || salaryField.getText().isBlank()
                            ? 0.0
                            : Double.parseDouble(salaryField.getText())
            );
        } catch (NumberFormatException e) {
            employeeToSave.setSalary(0.0);
        }

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                // Εδώ γίνεται η δουλειά στο background. Αν αποτύχει, πετάει Exception
                // το οποίο πιάνουμε στο setOnFailed
                employeeService.saveEmployee(employeeToSave);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            fadeOutLoading();
            clearForm();
            showTable();
            loadData();
            showInfoAlert("Success", "Employee saved successfully!");
        });

        // --- ΕΔΩ ΕΙΝΑΙ Η ΑΛΛΑΓΗ ΓΙΑ ΤΟ DUPLICATE KEY ---
        saveTask.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            currentEditingEmployee = null;

            Throwable ex = saveTask.getException(); // Παίρνουμε το λάθος
            String errorMessage = "Unexpected error";

            // Έλεγχος αν είναι σφάλμα βάσης (Duplicate Key)
            if (ex instanceof DataIntegrityViolationException) {
                // Παίρνουμε το πιο συγκεκριμένο μήνυμα (π.χ. από την PostgreSQL)
                String specificError = ((DataIntegrityViolationException) ex).getMostSpecificCause().getMessage();

                if (specificError != null && specificError.contains("employees_ssn_key")) {
                    errorMessage = "This SSN already exists for another employee!";
                } else if (specificError != null && specificError.contains("employees_email_key")) {
                    errorMessage = "This Email already exists!";
                } else {
                    errorMessage = "Database constraint violation.";
                }
            } else {
                // Οποιοδήποτε άλλο σφάλμα
                errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error occurred.";
            }

            showErrorAlert("Save Failed", errorMessage);
        });

        new Thread(saveTask).start();
    }

    // --- DELETE / DEACTIVATE ACTION ---

    private void handleDelete(Employee emp) {
        // Έλεγχος αν είναι ήδη ανενεργός
        if (emp.getExitDate() != null) {
            showErrorAlert("Action Invalid", "This employee is already inactive (contract terminated).");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deactivation");
        confirm.setHeaderText("Terminate contract for " + emp.getLastName() + "?");
        confirm.setContentText("This will mark the employee as inactive. They will remain in history.");
        styleDialog(confirm);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Κλήση της Soft Delete μεθόδου
                employeeService.softDeleteEmployee(emp.getId());

                loadData(); // Ανανέωση για να φανεί γκριζαρισμένο
                showInfoAlert("Success", "Employee marked as inactive.");
            } catch (Exception e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    }

    // --- UI HELPERS ---

    // Μέθοδος για να γκριζάρει τις γραμμές των απολυμένων
    private void setupRowStyling() {
        employeeTable.setRowFactory(tv -> new TableRow<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getExitDate() != null) {
                    // Γκρι χρώμα και italic για τους ανενεργούς
                    setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #9CA3AF; -fx-font-style: italic;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void fadeOutLoading() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
        fadeOut.play();
    }

    private boolean validateForm() {
        boolean isValid = true;
        TextField[] required = {firstNameField, lastNameField, emailField, ssnField};

        for (TextField f : required) {
            if (f.getText() == null || f.getText().trim().isEmpty()) {
                f.getStyleClass().add("text-field-error");
                isValid = false;
            }
        }

        if (hireDateField.getValue() == null) {
            hireDateField.getStyleClass().add("text-field-error");
            isValid = false;
        }
        return isValid;
    }

    private void resetFieldStyles() {
        firstNameField.getStyleClass().remove("text-field-error");
        lastNameField.getStyleClass().remove("text-field-error");
        emailField.getStyleClass().remove("text-field-error");
        ssnField.getStyleClass().remove("text-field-error");
        hireDateField.getStyleClass().remove("text-field-error");
    }

    private void setupValidationListeners() {
        TextField[] fields = {firstNameField, lastNameField, emailField, ssnField};
        for (TextField f : fields) {
            f.textProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) f.getStyleClass().remove("text-field-error");
            });
        }
    }

    private void setupTableColumns() {
        // ... (οι υπόλοιπες στήλες ίδιες) ...
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colDepartment.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDepartment() != null ? cell.getValue().getDepartment().getName() : "-"));

        // --- DYNAMIC ACTIONS COLUMN ---
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            // Αυτό το κουμπί θα αλλάζει ρόλο (Delete ή Rehire)
            private final Button btnAction = new Button();
            private final HBox pane = new HBox(5, btnEdit, btnAction);

            {
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");

                // Edit Logic (Ίδιο)
                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Employee emp = getTableView().getItems().get(getIndex());

                    // Δυναμική αλλαγή κουμπιού
                    if (emp.getExitDate() == null) {
                        // --- ΕΝΕΡΓΟΣ ΥΠΑΛΛΗΛΟΣ: Δείξε DELETE ---
                        btnAction.setText("Delete");
                        btnAction.getStyleClass().clear();
                        btnAction.getStyleClass().addAll("table-btn", "table-btn-delete");

                        btnAction.setOnAction(e -> handleDelete(emp));

                    } else {
                        // --- ΑΝΕΝΕΡΓΟΣ ΥΠΑΛΛΗΛΟΣ: Δείξε REHIRE ---
                        btnAction.setText("Rehire");
                        btnAction.getStyleClass().clear();
                        btnAction.getStyleClass().addAll("table-btn", "table-btn-rehire");


                        btnAction.setOnAction(e -> handleRehire(emp));
                    }

                    setGraphic(pane);
                }
            }
        });
    }

    // --- NEW REHIRE METHOD ---
    private void handleRehire(Employee emp) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rehire Employee");
        confirm.setHeaderText("Re-activate contract for " + emp.getLastName() + "?");
        confirm.setContentText("This will remove the exit date and make the employee active again.");
        styleDialog(confirm);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Καλούμε το service
                employeeService.rehireEmployee(emp.getId());

                // Ανανεώνουμε τον πίνακα
                loadData();
                showInfoAlert("Success", "Employee has been rehired successfully!");

            } catch (Exception e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    }

    private void setupDepartmentCombo() {
        departmentCombo.getItems().setAll(departmentService.getAllDepartments());
        departmentCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Department d) { return d == null ? "" : d.getName(); }
            @Override public Department fromString(String s) { return null; }
        });
    }

    private void applyFilter(String query) {
        filteredData.setPredicate(emp -> {
            if (query == null || query.isEmpty()) return true;
            String lower = query.toLowerCase();
            return emp.getLastName().toLowerCase().contains(lower) || emp.getFirstName().toLowerCase().contains(lower);
        });
    }

    private void showTable() { tableViewContainer.setVisible(true); formViewContainer.setVisible(false); }

    private void showForm(Employee emp) {
        this.currentEditingEmployee = emp;
        if (emp == null) {
            formTitle.setText("New Employee");
            clearForm();
        } else {
            formTitle.setText("Edit Employee");
            firstNameField.setText(emp.getFirstName());
            lastNameField.setText(emp.getLastName());
            emailField.setText(emp.getEmail());
            phoneField.setText(emp.getPhone());
            ssnField.setText(emp.getSsn());
            salaryField.setText(String.valueOf(emp.getSalary()));
            addressField.setText(emp.getAddress());
            departmentCombo.setValue(emp.getDepartment());
            if (hireDateField != null) hireDateField.setValue(emp.getHireDate());
        }
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(true);
    }

    @FXML public void handleAddEmployee() { showForm(null); }
    @FXML public void handleBackToTable() { showTable(); }
    @FXML public void handleRefresh() { loadData(); }

    private void clearForm() {
        firstNameField.clear(); lastNameField.clear(); emailField.clear();
        phoneField.clear(); ssnField.clear(); salaryField.clear();
        if (hireDateField != null) hireDateField.setValue(null);
        departmentCombo.setValue(null); resetFieldStyles();
        phoneField.clear(); addressField.clear();
        currentEditingEmployee = null;
    }

    private void showInfoAlert(String title, String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg); styleDialog(a); a.show(); }
    private void showErrorAlert(String title, String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg); styleDialog(a); a.show(); }
    private void styleDialog(Dialog<?> d) { try { d.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm()); } catch (Exception e) {} }
}