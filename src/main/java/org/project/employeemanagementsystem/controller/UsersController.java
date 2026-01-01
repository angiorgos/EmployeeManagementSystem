package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.RoleService;
import org.project.employeemanagementsystem.service.UserService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;
//x
@Controller
public class UsersController implements Initializable {

    @Autowired private UserService userService; // Σύνδεση για Χρήστες
    @Autowired private RoleService roleService; // Σύνδεση για Ρόλους (στο dropdown)
    @Autowired private UserSession userSession; // connection to user session

   // @FXML private TableView<User> usersTable;
    @FXML private ComboBox<Role> roleComboBox; // Για επιλογή ρόλου (ADMIN, EMPLOYEE, HR, ACCOUNTANT)

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;

    @FXML private TextField usernameTextField;

    @FXML private Button createUserButton;
    @FXML private Button deleteUserButton;
    @FXML Button editUserButton;




    private ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

       // --- 1. Initialize TableView columns ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(cellData -> {
            Role role = cellData.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.getName() : "");
        });

        // Φόρτωση Χρηστών στον Πίνακα
        userList.setAll(userService.getAllUsers());
        usersTable.setItems(userList);

        // Φόρτωση Ρόλων στο Dropdown
        if (roleComboBox != null) {
            roleComboBox.getItems().setAll(roleService.getAllRoles());
        }


        //seting the right tab for usename and password
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Set username in text field
                usernameTextField.setText(newSelection.getUsername());

                // Set role in combo box
                Role role = newSelection.getRole();
                if (role != null) {
                    roleComboBox.getSelectionModel().select(role);
                } else {
                    roleComboBox.getSelectionModel().clearSelection();
                }
            } else {
                // Clear fields if nothing selected
                usernameTextField.clear();
                roleComboBox.getSelectionModel().clearSelection();
            }
        });


        // Disable delete button initially
        deleteUserButton.setDisable(true);

        // Enable delete button only when a user is selected
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteUserButton.setDisable(newSelection == null);
        });


        editUserButton.setDisable(true);

// Enable edit button only when a user is selected
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean selectedNotNull = newSelection != null;
            editUserButton.setDisable(!selectedNotNull);
        });


    }



    @FXML
    private void onUserTableClick() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Show username in TextField
            usernameTextField.setText(selected.getUsername());

            // Show role in ComboBox
            Role role = selected.getRole();
            if (role != null) {
                roleComboBox.getSelectionModel().select(role);
            } else {
                roleComboBox.getSelectionModel().clearSelection();
            }
        }
    }



    @FXML
    private void onCreateUser() {
        // Create dialog
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.setHeaderText("Enter user details");

        // Buttons
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        ComboBox<Role> roleField = new ComboBox<>();
        roleField.getItems().setAll(roleService.getAllRoles());

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Enable Create button only when all fields are filled
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        usernameField.textProperty().addListener((obs, oldVal, newVal) ->
                createButton.setDisable(newVal.trim().isEmpty() || passwordField.getText().trim().isEmpty() || roleField.getValue() == null)
        );
        passwordField.textProperty().addListener((obs, oldVal, newVal) ->
                createButton.setDisable(newVal.trim().isEmpty() || usernameField.getText().trim().isEmpty() || roleField.getValue() == null)
        );
        roleField.valueProperty().addListener((obs, oldVal, newVal) ->
                createButton.setDisable(newVal == null || usernameField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty())
        );

        // Convert result
        dialog.setResultConverter(button -> {
            if (button == createButtonType) {
                User user = new User();
                user.setUsername(usernameField.getText().trim());
                user.setPassword(passwordField.getText().trim());
                user.setRole(roleField.getValue());
                return user;
            }
            return null;
        });

        // Show dialog and save user
        dialog.showAndWait().ifPresent(user -> {
            try {
                userService.saveUser(user);
                userList.add(user);
                usersTable.refresh();
            } catch (RuntimeException e) {
                showAlert("Error", e.getMessage());
            }
        });
    }




    @FXML
    private void onDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        User currentUser = userSession.getCurrentUser();
        boolean currentUserIsAdmin = currentUser != null && currentUser.getRole() != null
                && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (!currentUserIsAdmin) {
            // Prompt for password of the user to be deleted
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Password Required");
            passwordDialog.setHeaderText("Enter password of user: " + selected.getUsername());
            passwordDialog.setContentText("Password:");

            passwordDialog.showAndWait().ifPresent(password -> {
                if (!selected.getPassword().equals(password)) {
                    showAlert("Error", "Incorrect password! Cannot delete user.");
                } else {
                    confirmAndDeleteUser(selected);
                }
            });
        } else {
            // Admin can delete without password
            confirmAndDeleteUser(selected);
        }
    }

    // Helper method to confirm deletion
    private void confirmAndDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete User");
        confirmation.setHeaderText("Are you sure you want to delete this user?");
        confirmation.setContentText(user.getUsername());

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.deleteUser(user);
                    userList.remove(user);
                    usersTable.refresh();
                } catch (RuntimeException e) {
                    showAlert("Error", e.getMessage());
                }
            }
        });
    }



    @FXML
    private void onEditUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean admin = isAdmin();

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user details");

        ButtonType editButtonType = new ButtonType("Edit User", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(editButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField(selected.getUsername());

        ComboBox<Role> roleField = new ComboBox<>();
        roleField.getItems().setAll(roleService.getAllRoles());
        roleField.getSelectionModel().select(selected.getRole());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(roleField, 1, 1);

        // Password only for ADMIN
        if (admin) {
            grid.add(new Label("Password:"), 0, 2);
            grid.add(passwordField, 1, 2);
        }

        dialog.getDialogPane().setContent(grid);

        // Disable Edit button if invalid
        Button editButton = (Button) dialog.getDialogPane().lookupButton(editButtonType);
        editButton.setDisable(true);

        Runnable validate = () -> {
            boolean usernameValid = !usernameField.getText().trim().isEmpty();
            boolean roleValid = roleField.getValue() != null;
            editButton.setDisable(!(usernameValid && roleValid));
        };

        usernameField.textProperty().addListener((o, a, b) -> validate.run());
        roleField.valueProperty().addListener((o, a, b) -> validate.run());

        validate.run();

        dialog.setResultConverter(button -> {
            if (button == editButtonType) {
                selected.setUsername(usernameField.getText().trim());
                selected.setRole(roleField.getValue());

                // Only admin can change password
                if (admin && !passwordField.getText().trim().isEmpty()) {
                    selected.setPassword(passwordField.getText().trim());
                }

                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            try {
                userService.saveUser(user);
                usersTable.refresh();
            } catch (RuntimeException e) {
                showAlert("Error", e.getMessage());
            }
        });
    }




    private boolean isAdmin() {
        User current = userSession.getCurrentUser();
        return current != null
                && current.getRole() != null
                && "ADMIN".equalsIgnoreCase(current.getRole().getName());
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }








}