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

    // --- VIEWS & OVERLAYS ---
    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay;
    @FXML private Label formTitle;

    // --- TABLE ---
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Void> actionCol;

    // // --- FORM ---
    @FXML private TextField searchField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private ComboBox<Employee> employeeComboBox;

    private FilteredList<User> filteredData;
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Initial state (same as DepartmentController)
        loadingOverlay.setVisible(true);
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(false);

        setupTableColumns();
        loadRoles();
        loadEmployees();

        Platform.runLater(this::loadUsers);

        searchField.textProperty().addListener(
                (obs, oldVal, newVal) -> applyFilter(newVal)
        );
    }

    /* ===================== DATA LOADING ===================== */

    private void loadUsers() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1);

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getAllUsers();
            }
        };

        task.setOnSucceeded(event -> {
            filteredData = new FilteredList<>(
                    FXCollections.observableArrayList(task.getValue()),
                    u -> true
            );
            usersTable.setItems(filteredData);

            addActionButtonsToTable();
            applyFilter(searchField.getText());

            PauseTransition delay = new PauseTransition(Duration.seconds(0.3));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(
                        Duration.seconds(0.5), loadingOverlay
                );
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(x -> loadingOverlay.setVisible(false));
                fade.play();
                tableViewContainer.setVisible(true);
            });
            delay.play();
        });

        task.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to load users: " + task.getException().getMessage());
            styleDialog(alert);
            alert.show();
        });

        new Thread(task).start();
    }

    /* ===================== TABLE SETUP ===================== */

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(cell -> {
            Role role = cell.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(
                    role != null ? role.getName() : ""
            );
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) {
            filteredData.setPredicate(u -> true);
        } else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(
                    u -> u.getUsername().toLowerCase().contains(lower)
            );
        }
    }

    /* ===================== COMBOBOX LOADERS ===================== */

    private void loadRoles() {
        roleComboBox.getItems().setAll(roleService.getAllRoles());
        roleComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Role r) {
                return r == null ? "" : r.getName();
            }
            @Override public Role fromString(String s) { return null; }
        });
    }

    private void loadEmployees() {
        employeeComboBox.getItems().setAll(employeeService.getActiveEmployees());
        employeeComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) {
                return e == null ? "" : e.getFirstName() + " " + e.getLastName();
            }
            @Override public Employee fromString(String s) { return null; }
        });
    }

    /* ===================== FORM ACTIONS ===================== */

    @FXML
    public void onCreateUser() {
        selectedUser = null;
        formTitle.setText("New User");
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue(null);
        employeeComboBox.setValue(null);

        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(true);
    }

    @FXML
    public void onSaveUser() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()
                || roleComboBox.getValue() == null
                || employeeComboBox.getValue() == null) {

            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "All fields except password are required.");
            styleDialog(alert);
            alert.show();
            return;
        }

        if (selectedUser == null) selectedUser = new User();

        selectedUser.setUsername(username);
        selectedUser.setRole(roleComboBox.getValue());
        selectedUser.setEmployee(employeeComboBox.getValue());

        if (!passwordField.getText().isBlank()) {
            selectedUser.setPassword(passwordField.getText());
        }

        userService.saveUser(selectedUser);

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "User saved successfully!");
        styleDialog(alert);
        alert.showAndWait();

        handleRefresh();
        handleBackToTable();
    }

    @FXML
    public void handleBackToTable() {
        formViewContainer.setVisible(false);
        tableViewContainer.setVisible(true);
    }

    @FXML
    public void handleRefresh() {
        loadUsers();
    }

    /* ===================== ACTION COLUMN ===================== */

    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                pane.getStyleClass().add("action-box");
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");

                btnEdit.setOnAction(e -> {
                    selectedUser = getTableView().getItems().get(getIndex());
                    formTitle.setText("Edit User");
                    usernameField.setText(selectedUser.getUsername());
                    roleComboBox.setValue(selectedUser.getRole());
                    employeeComboBox.setValue(selectedUser.getEmployee());
                    passwordField.clear();

                    tableViewContainer.setVisible(false);
                    formViewContainer.setVisible(true);
                });

                btnDelete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete user: " + user.getUsername() + "?");
                    styleDialog(confirm);

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        userService.deleteUser(user);
                        loadUsers();

                        Alert info = new Alert(Alert.AlertType.INFORMATION,
                                "User deleted.");
                        styleDialog(info);
                        info.show();
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

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
