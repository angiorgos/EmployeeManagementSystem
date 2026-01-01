package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class LeavesController2 {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @FXML
    private FlowPane requestsFlowPane;

    @FXML
    public void initialize() {
        // Load all leave requests from DB
        List<LeaveRequest> leaveRequests = leaveRequestService.getAllRequests();

        for (LeaveRequest request : leaveRequests) {
            VBox requestBox = new VBox(5); // spacing = 5
            requestBox.setStyle("-fx-padding: 10; -fx-border-color: gray; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");

            // Title
            Label title = new Label("Request " + request.getId());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            // Employee info
            Label nameLabel = new Label("Name: " + request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
            Label phoneLabel = new Label("Phone: " + request.getEmployee().getPhone());
            Label emailLabel = new Label("Email: " + request.getEmployee().getEmail());

            // Reason button
            Button reasonBtn = new Button("Show Reason");
            reasonBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Leave Reason");
                alert.setHeaderText("Reason for leave request " + request.getId());
                alert.setContentText(request.getReason());
                alert.showAndWait();
            });

            // Status ComboBox
            ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("Pending", "Accepted", "Rejected"));
            statusCombo.setValue(request.getStatus() != null ? request.getStatus().name() : "Pending");
            statusCombo.setOnAction(e -> {
                String selected = statusCombo.getValue().toUpperCase();
                try {
                    request.setStatus(LeaveStatus.valueOf(selected));
                    leaveRequestService.submitRequest(request); // save change
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Could not update status");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            // Add all nodes to VBox
            requestBox.getChildren().addAll(title, nameLabel, phoneLabel, emailLabel, reasonBtn, statusCombo);

            // Add VBox to FlowPane
            requestsFlowPane.getChildren().add(requestBox);
        }
    }
}
