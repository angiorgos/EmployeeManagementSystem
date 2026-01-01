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
import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
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

    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay;
    @FXML private Label formTitle;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Void> actionCol;

    @FXML private TextField searchField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;

    private FilteredList<User> filteredData;
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadRoles();
        Platform.runLater(this::loadUsers);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    private void loadUsers() {
        loadingOverlay.setVisible(true);
        tableViewContainer.setVisible(false);

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getAllUsers();
            }
        };

        task.setOnSucceeded(event -> {
            filteredData = new FilteredList<>(FXCollections.observableArrayList(task.getValue()));
            usersTable.setItems(filteredData);
            applyFilter(searchField.getText());
            addActionButtonsToTable();

            // Fade out loading overlay
            PauseTransition delay = new PauseTransition(Duration.seconds(0.3));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(evt -> loadingOverlay.setVisible(false));
                fade.play();
                tableViewContainer.setVisible(true);
            });
            delay.play();
        });

        task.setOnFailed(event -> loadingOverlay.setVisible(false));
        new Thread(task).start();
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleCol.setCellValueFactory(cell -> {
            Role role = cell.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role.getName() : "");
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) filteredData.setPredicate(u -> true);
        else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(u -> u.getUsername().toLowerCase().contains(lower));
        }
    }

    private void loadRoles() {
        roleComboBox.getItems().setAll(roleService.getAllRoles());
        roleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) { return role != null ? role.getName() : ""; }
            @Override
            public Role fromString(String s) { return null; }
        });
    }

    @FXML
    private void onCreateUser() {
        showForm(null);
    }

    @FXML
    private void handleBackToTable() {
        formViewContainer.setVisible(false);
        tableViewContainer.setVisible(true);
    }

    @FXML
    private void onSaveUser() {
        String username = usernameField.getText().trim();
        Role role = roleComboBox.getValue();
        String password = passwordField.getText();

        if(username.isEmpty() || role == null) return;

        if(selectedUser == null) selectedUser = new User();
        selectedUser.setUsername(username);
        selectedUser.setRole(role);
        if(password != null && !password.isEmpty()) selectedUser.setPassword(password);

        userService.saveUser(selectedUser);
        loadUsers();
        handleBackToTable();
    }

    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                pane.getStyleClass().add("action-box");
                btnEdit.getStyleClass().addAll("table-btn", "table-btn-edit");
                btnDelete.getStyleClass().addAll("table-btn", "table-btn-delete");

                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user: " + user.getUsername() + "?");
                    confirm.showAndWait().ifPresent(r -> { if(r==ButtonType.OK) { userService.deleteUser(user); loadUsers(); }});
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void showForm(User user) {
        selectedUser = user;
        if(user == null){
            formTitle.setText("New User");
            usernameField.clear();
            passwordField.clear();
            roleComboBox.setValue(null);
        } else {
            formTitle.setText("Edit User");
            usernameField.setText(user.getUsername());
            roleComboBox.setValue(user.getRole());
        }
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(true);
    }

    @FXML
    private void handleRefresh() { loadUsers(); }
}
