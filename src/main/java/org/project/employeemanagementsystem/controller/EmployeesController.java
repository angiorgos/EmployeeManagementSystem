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

    private final ObservableList<Employee> masterData = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredData;
    private Employee currentEditingEmployee = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupDepartmentCombo();
        setupValidationListeners();

        filteredData = new FilteredList<>(masterData, p -> true);
        employeeTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));

        Platform.runLater(this::loadData);
    }

    private void loadData() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() throws Exception {
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

    @FXML
    public void handleSaveEmployee() {

        resetFieldStyles();
        if (!validateForm()) return;

        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Employee employeeToSave = new Employee();

        if (currentEditingEmployee != null) {
            employeeToSave.setId(currentEditingEmployee.getId());
        }

        employeeToSave.setFirstName(firstNameField.getText());
        employeeToSave.setLastName(lastNameField.getText());
        employeeToSave.setEmail(emailField.getText());
        employeeToSave.setPhone(phoneField.getText());
        employeeToSave.setSsn(ssnField.getText());
        employeeToSave.setDepartment(departmentCombo.getValue());
        employeeToSave.setHireDate(hireDateField.getValue());

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

        saveTask.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            currentEditingEmployee = null;
            Throwable ex = saveTask.getException();
            ex.printStackTrace();
            showErrorAlert("Save Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
        });

        new Thread(saveTask).start();
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

        TextField[] required = {
                firstNameField, lastNameField, emailField, ssnField
        };

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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colDepartment.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDepartment() != null ? cell.getValue().getDepartment().getName() : "-"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");
                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleDelete(Employee emp) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + emp.getLastName() + "?", ButtonType.YES, ButtonType.NO);
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            employeeService.deleteEmployee(emp.getId());
            loadData();
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
        currentEditingEmployee = null;
    }

    private void showInfoAlert(String title, String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg); styleDialog(a); a.show(); }
    private void showErrorAlert(String title, String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg); styleDialog(a); a.show(); }
    private void styleDialog(Dialog<?> d) { try { d.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm()); } catch (Exception e) {} }
}