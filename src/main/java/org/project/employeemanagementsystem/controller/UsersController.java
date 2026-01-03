package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.RoleService;
import org.project.employeemanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class UsersController implements Initializable {

    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private EmployeeService employeeService;

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

    // --- PROFILE PICTURE FIELDS ---
    @FXML private Circle avatarCircle;
    @FXML private FontIcon defaultIcon;

    private byte[] currentImageBytes = null; // Προσωρινή αποθήκευση bytes για τη βάση

    // --- DATA LISTS ---
    private ObservableList<User> masterData = FXCollections.observableArrayList();
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

        // Σύνδεση του πίνακα με το masterData
        filteredData = new FilteredList<>(masterData, u -> true);
        usersTable.setItems(filteredData);

        Platform.runLater(this::loadUsers);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    // -------------------- IMAGE UPLOAD LOGIC --------------------
    @FXML
    public void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        // Χρήση null για αποφυγή NullPointerException
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                currentImageBytes = Files.readAllBytes(selectedFile.toPath());

                Image image = new Image(new ByteArrayInputStream(currentImageBytes));

                if (image.isError()) {
                    showAlert(Alert.AlertType.ERROR, "Error loading image", "The selected file could not be loaded.");
                    return;
                }

                avatarCircle.setFill(new ImagePattern(image));
                avatarCircle.setVisible(true);
                defaultIcon.setVisible(false);

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Failed to read file: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleClearPhoto() {
        if (selectedUser == null && currentImageBytes == null) {
            clearPhotoUI();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Photo");
        alert.setHeaderText("Remove profile picture?");
        alert.setContentText("Are you sure you want to remove the profile picture?");
        styleDialog(alert);

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (selectedUser != null && selectedUser.getId() != null) {
                    userService.removeProfilePicture(selectedUser);
                }

                clearPhotoUI();
                currentImageBytes = null;
                showAlert(Alert.AlertType.INFORMATION, "Photo removed.");

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error removing photo: " + e.getMessage());
            }
        }
    }

    private void clearPhotoUI() {
        avatarCircle.setFill(null);
        avatarCircle.setVisible(false);
        defaultIcon.setVisible(true);
    }

    // -------------------- LOAD USERS --------------------
    private void loadUsers() {
        loadingOverlay.setVisible(true);
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() { return userService.getAllUsers(); }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue()); // Ενημέρωση της λίστας
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

    // -------------------- TABLE --------------------
    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(cell -> {
            Role role = cell.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role.getName() : "");
        });
        employeeCol.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            return new javafx.beans.property.SimpleStringProperty(
                    emp != null ? emp.getFirstName() + " " + emp.getLastName() + " (ID: " + emp.getId() + ")" : ""
            );
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) filteredData.setPredicate(u -> true);
        else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(u -> u.getUsername().toLowerCase().contains(lower));
        }
    }

    // -------------------- COMBOBOX --------------------
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
            @Override public String toString(Employee e) {
                return e != null ? e.getFirstName() + " " + e.getLastName() + " (ID: " + e.getId() + ")" : "";
            }
            @Override public Employee fromString(String s) { return null; }
        });
    }

    // -------------------- FORM --------------------
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

        // 1. Ενημέρωση πεδίων User
        selectedUser.setUsername(usernameField.getText().trim());
        selectedUser.setRole(roleComboBox.getValue());

        if (currentImageBytes != null) {
            selectedUser.setProfilePicture(currentImageBytes);
        }

        if (!passwordField.getText().isBlank()) {
            selectedUser.setPassword(passwordField.getText());
        }

        // 2. Έλεγχος Employee
        Employee selectedEmp = employeeComboBox.getValue();
        if (selectedEmp == null || selectedEmp.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Selected employee is invalid!");
            return;
        }

        Employee empFromDb = employeeService.getEmployeeById(selectedEmp.getId())
                .orElse(null);

        if (empFromDb == null) {
            showAlert(Alert.AlertType.ERROR, "Employee not found in database!");
            return;
        }

        // --- VALIDATION: Check if employee is already linked ---
        if (empFromDb.getUser() != null) {
            Long existingOwnerId = empFromDb.getUser().getId();
            Long currentUserId = selectedUser.getId();

            if (currentUserId == null || !existingOwnerId.equals(currentUserId)) {
                showAlert(Alert.AlertType.ERROR,
                        "Action Denied",
                        "This employee is already linked to user: '" + empFromDb.getUser().getUsername() + "'.\n" +
                                "Please unlink the employee from the other user first.");
                return;
            }
        }

        try {
            // ΒΗΜΑ 1: Αποθήκευση User
            User savedUser = userService.saveUser(selectedUser);

            // ΒΗΜΑ 2: Αποσύνδεση παλιού
            if (savedUser.getEmployee() != null && !savedUser.getEmployee().getId().equals(empFromDb.getId())) {
                Employee oldEmp = savedUser.getEmployee();
                oldEmp.setUser(null);
                employeeService.saveEmployee(oldEmp);
            }

            // ΒΗΜΑ 3: Σύνδεση νέου
            empFromDb.setUser(savedUser);
            employeeService.saveEmployee(empFromDb);

            // REFRESH: Εδώ κάνουμε πλήρη ανανέωση για να είμαστε σίγουροι
            handleRefresh();
            handleBackToTable();

            showAlert(Alert.AlertType.INFORMATION, "User saved successfully!");

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error saving user: " + ex.getMessage());
        }
    }

    private boolean validateForm() {
        if (usernameField.getText().trim().isEmpty()
                || roleComboBox.getValue() == null
                || employeeComboBox.getValue() == null) {
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
        clearPhotoUI();
        currentImageBytes = null;
    }

    private void showForm(boolean show) {
        formViewContainer.setVisible(show);
        tableViewContainer.setVisible(!show);

        if (show && selectedUser != null) {
            if (selectedUser.getProfilePicture() != null && selectedUser.getProfilePicture().length > 0) {
                Image img = new Image(new ByteArrayInputStream(selectedUser.getProfilePicture()));
                avatarCircle.setFill(new ImagePattern(img));
                avatarCircle.setVisible(true);
                defaultIcon.setVisible(false);
            } else {
                avatarCircle.setVisible(false);
                defaultIcon.setVisible(true);
            }
            currentImageBytes = null;
        }
        else if (show) {
            avatarCircle.setVisible(false);
            defaultIcon.setVisible(true);
            currentImageBytes = null;
        }
    }

    @FXML public void handleBackToTable() { showForm(false); }
    @FXML public void handleRefresh() { loadUsers(); }

    // -------------------- ACTION BUTTONS --------------------
    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                pane.getStyleClass().add("action-box");
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");

                btnEdit.setOnAction(event -> {
                    selectedUser = getTableView().getItems().get(getIndex());
                    formTitle.setText("Edit User");
                    usernameField.setText(selectedUser.getUsername());
                    roleComboBox.setValue(selectedUser.getRole());
                    employeeComboBox.setValue(selectedUser.getEmployee());
                    passwordField.clear();
                    showForm(true);
                });

                btnDelete.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete user: " + user.getUsername() + "?");
                    styleDialog(confirm);

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        try {
                            userService.deleteUser(user);

                            // ΑΛΛΑΓΗ ΕΔΩ: Επιστροφή στο κλασικό refresh από τη βάση
                            loadUsers();

                            showAlert(Alert.AlertType.INFORMATION, "User deleted.");
                        } catch (Exception e) {
                            showAlert(Alert.AlertType.ERROR, "Error deleting user: " + e.getMessage());
                        }
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

    // --- HELPER METHODS ---

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setContentText(msg);
        alert.setHeaderText(null);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.setHeaderText(null);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm()); }
        catch (Exception ignored) {}
    }
}