package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class TopbarController implements Initializable {

    @Autowired
    private UserSession userSession;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateUserDisplay();
    }

    private void updateUserDisplay() {
        User currentUser = userSession.getCurrentUser();

        if (currentUser != null) {
            if (currentUser.getEmployee() != null) {
                String fullName = currentUser.getEmployee().getFirstName() + " " +
                        currentUser.getEmployee().getLastName();
                userNameLabel.setText(fullName);
            } else {
                userNameLabel.setText(currentUser.getUsername());
            }

            if (currentUser.getRole() != null) {
                userRoleLabel.setText(currentUser.getRole().getName());
            } else {
                userRoleLabel.setText("-");
            }
        }
    }
}