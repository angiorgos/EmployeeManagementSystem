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
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern; // <--- ΣΗΜΑΝΤΙΚΟ IMPORT
import javafx.scene.shape.Circle;       // <--- ΣΗΜΑΝΤΙΚΟ IMPORT
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
    // ΑΛΛΑΓΗ: Χρησιμοποιούμε Circle αντί για ImageView για τέλειο στρογγυλό σχήμα
    @FXML private Circle avatarCircle;
    @FXML private FontIcon defaultIcon;

    private byte[] currentImageBytes = null; // Προσωρινή αποθήκευση bytes για τη βάση

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

    // -------------------- IMAGE UPLOAD LOGIC --------------------
    @FXML
    public void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(tableViewContainer.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // 1. Διάβασε το αρχείο σε bytes (για αποθήκευση στη βάση)
                currentImageBytes = Files.readAllBytes(selectedFile.toPath());

                // 2. Εμφάνισε το στο UI χρησιμοποιώντας ImagePattern στο Circle
                Image image = new Image(new ByteArrayInputStream(currentImageBytes));
                avatarCircle.setFill(new ImagePattern(image));

                // 3. Εμφάνισε τον κύκλο, κρύψε το εικονίδιο
                avatarCircle.setVisible(true);
                defaultIcon.setVisible(false);

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Failed to load image: " + e.getMessage());
            }
        }
    }

    // -------------------- LOAD USERS --------------------
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

        selectedUser.setUsername(usernameField.getText().trim());
        selectedUser.setRole(roleComboBox.getValue());

        // *** SAVE PROFILE PICTURE ***
        // Αν έχουμε επιλέξει νέα εικόνα, την περνάμε στο αντικείμενο
        if (currentImageBytes != null) {
            selectedUser.setProfilePicture(currentImageBytes);
        }

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

        // Unlink old employee logic
        if (selectedUser.getEmployee() != null && !selectedUser.getEmployee().getId().equals(empFromDb.getId())) {
            Employee oldEmp = selectedUser.getEmployee();
            oldEmp.setUser(null);
            employeeService.saveEmployee(oldEmp);
        }

        // Link new employee
        empFromDb.setUser(selectedUser);
        selectedUser.setEmployee(empFromDb);

        if (!passwordField.getText().isBlank()) {
            selectedUser.setPassword(passwordField.getText());
        }

        try {
            employeeService.saveEmployee(empFromDb);
            userService.saveUser(selectedUser);

            showAlert(Alert.AlertType.INFORMATION, "User saved successfully!");
            handleRefresh();
            handleBackToTable();
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

        // Reset Image
        avatarCircle.setFill(null);
        avatarCircle.setVisible(false);
        defaultIcon.setVisible(true);
        currentImageBytes = null;
    }

    private void showForm(boolean show) {
        formViewContainer.setVisible(show);
        tableViewContainer.setVisible(!show);

        // Αν μπαίνουμε στη φόρμα για EDIT
        if (show && selectedUser != null) {
            if (selectedUser.getProfilePicture() != null && selectedUser.getProfilePicture().length > 0) {
                // Φόρτωση από τη βάση -> Circle
                Image img = new Image(new ByteArrayInputStream(selectedUser.getProfilePicture()));
                avatarCircle.setFill(new ImagePattern(img));

                avatarCircle.setVisible(true);
                defaultIcon.setVisible(false);
            } else {
                avatarCircle.setVisible(false);
                defaultIcon.setVisible(true);
            }
            // Καθαρίζουμε τα bytes ώστε να μην κάνουμε overwrite αν ο χρήστης δεν αλλάξει εικόνα
            currentImageBytes = null;
        }
        else if (show) {
            // New User Mode
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
                        userService.deleteUser(user);
                        loadUsers();
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

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm()); }
        catch (Exception ignored) {}
    }
}