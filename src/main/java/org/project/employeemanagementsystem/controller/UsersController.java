package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.RoleService;
import org.project.employeemanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class UsersController implements Initializable {

    @Autowired private UserService userService; // Σύνδεση για Χρήστες
    @Autowired private RoleService roleService; // Σύνδεση για Ρόλους (στο dropdown)

    @FXML private TableView<User> usersTable;
    @FXML private ComboBox<Role> roleComboBox; // Για επιλογή ρόλου (ADMIN, EMPLOYEE, HR, ACCOUNTANT)

    private ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Φόρτωση Χρηστών στον Πίνακα
        userList.setAll(userService.getAllUsers());
        usersTable.setItems(userList);

        // Φόρτωση Ρόλων στο Dropdown
        if (roleComboBox != null) {
            roleComboBox.getItems().setAll(roleService.getAllRoles());
        }
    }
}