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
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.RoleService;
import org.project.employeemanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class UsersController implements Initializable {

    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private EmployeeService employeeService;

    // --- UI ELEMENTS ---
    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay;
    @FXML private Label formTitle;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> employeeCol;
    @FXML private TableColumn<User, Void> actionCol;

    @FXML private TextField searchField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private ComboBox<Employee> employeeComboBox;

    private FilteredList<User> filteredData;
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(false);
        loadingOverlay.setVisible(true);

        setupTableColumns();
        loadRoles();
        loadEmployees();

        Platform.runLater(this::loadUsers);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    // ------------------- LOAD USERS -------------------
    private void loadUsers() {
        loadingOverlay.setVisible(true);
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() { return userService.getAllUsers(); }
        };

        task.setOnSucceeded(e -> {
            filteredData = new FilteredList<>(FXCollections.observableArrayList(task.getValue()), u -> true);
            usersTable.setItems(filteredData);
            addActionButtonsToTable();
            applyFilter(searchField.getText());
            fadeOutLoading();
        });

        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            showAlert(Alert.AlertType.ERROR, "Failed to load users: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void fadeOutLoading() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0.3));
        delay.setOnFinished(ev -> {
            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(x -> loadingOverlay.setVisible(false));
            fade.play();
            tableViewContainer.setVisible(true);
        });
        delay.play();
    }

    // ------------------- TABLE -------------------
    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(cell -> {
            Role role = cell.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role.getName() : "");
        });
        employeeCol.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getFirstName() + " " + emp.getLastName() : "");
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) filteredData.setPredicate(u -> true);
        else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(u -> u.getUsername().toLowerCase().contains(lower));
        }
    }

    // ------------------- COMBOBOX -------------------
    private void loadRoles() {
        List<Role> roles = roleService.getAllRoles();
        roleComboBox.getItems().setAll(roles);
        roleComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Role r) { return r != null ? r.getName() : ""; }
            @Override public Role fromString(String s) { return null; }
        });
    }

    private void loadEmployees() {
        List<Employee> employees = employeeService.getActiveEmployees();
        employeeComboBox.getItems().setAll(employees);
        employeeComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) { return e != null ? e.getFirstName() + " " + e.getLastName() : ""; }
            @Override public Employee fromString(String s) { return null; }
        });
    }

    // ------------------- FORM -------------------
    @FXML
    public void onCreateUser() {
        selectedUser = null;
        formTitle.setText("New User");
        clearForm();
        showForm(true);
    }

    @FXML
    public void onSaveUser() {
        if (!validateForm()) return;

        if (selectedUser == null) selectedUser = new User();

        selectedUser.setUsername(usernameField.getText().trim());
        selectedUser.setRole(roleComboBox.getValue());
        selectedUser.setEmployee(employeeComboBox.getValue());

        if (!passwordField.getText().isBlank()) {
            selectedUser.setPassword(passwordField.getText());
        }

        try {
            userService.saveUser(selectedUser);
            showAlert(Alert.AlertType.INFORMATION, "User saved successfully!");
            handleRefresh();
            handleBackToTable();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error saving user: " + ex.getMessage());
        }
    }

    private boolean validateForm() {
        if (usernameField.getText().trim().isEmpty() || roleComboBox.getValue() == null || employeeComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "All fields except password are required.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue(null);
        employeeComboBox.setValue(null);
    }

    private void showForm(boolean show) {
        formViewContainer.setVisible(show);
        tableViewContainer.setVisible(!show);
    }

    @FXML
    public void handleBackToTable() { showForm(false); }
    @FXML
    public void handleRefresh() { loadUsers(); }

    // ------------------- ACTION BUTTONS -------------------
    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                // MATCH DEPARTMENT CONTROLLER STYLE
                pane.getStyleClass().add("action-box");
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");

                btnEdit.setOnAction(event -> {
                    selectedUser = getTableView().getItems().get(getIndex());
                    usernameField.setText(selectedUser.getUsername());
                    roleComboBox.setValue(selectedUser.getRole());
                    employeeComboBox.setValue(selectedUser.getEmployee());
                    passwordField.clear();
                    formTitle.setText("Edit User");
                    showForm(true);
                });

                btnDelete.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete " + user.getUsername() + "?");
                    styleDialog(confirm);
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        userService.deleteUser(user);
                        filteredData.getSource().remove(user);
                        showAlert(Alert.AlertType.INFORMATION, "User deleted.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
                setStyle("-fx-padding: 0;");
            }
        });
    }

    // ------------------- UTIL -------------------
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
