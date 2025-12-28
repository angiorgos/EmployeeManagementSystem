package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.util.Navigator;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    @Autowired
    protected Navigator navigator;

    @Autowired
    protected UserSession userSession;

    @FXML protected Label userNameLabel;
    @FXML protected Label userRoleLabel;

    public void userTitle() {
        if (userNameLabel == null || userRoleLabel == null) return;

        User currentUser = userSession.getCurrentUser();

        if (currentUser != null) {
            if (currentUser.getEmployee() != null) {
                String fullName = currentUser.getEmployee().getFirstName() + " " + currentUser.getEmployee().getLastName();
                userNameLabel.setText(fullName);
            } else {
                userNameLabel.setText(currentUser.getUsername());
            }
            if (currentUser.getRole() != null) {
                userRoleLabel.setText(currentUser.getRole().getName());
            }
        }
    }

}