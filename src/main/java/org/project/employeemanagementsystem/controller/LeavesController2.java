package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LeavesController2 {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @FXML
    private FlowPane requestsFlowPane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<LeaveStatus> statusFilterCombo;

    private List<LeaveRequest> allRequests;

    @FXML
    public void initialize() {
        // Load requests once
        allRequests = leaveRequestService.getAllRequests();

        // Setup status filter
        statusFilterCombo.setItems(
                FXCollections.observableArrayList(LeaveStatus.values())
        );
        statusFilterCombo.setValue(LeaveStatus.PENDING);

        // Listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refresh());

        refresh();
    }

    // ===== Refresh UI =====
    private void refresh() {
        requestsFlowPane.getChildren().clear();

        String search = searchField.getText().toLowerCase();
        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r ->
                        (selectedStatus == null || r.getStatus() == selectedStatus)
                                &&
                                (r.getEmployee().getFirstName() + " " +
                                        r.getEmployee().getLastName())
                                        .toLowerCase()
                                        .contains(search)
                )
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    // ===== Create Request Card =====
    private void createRequestCard(LeaveRequest request) {
        VBox box = new VBox(5);
        box.setStyle("""
                -fx-padding: 10;
                -fx-border-color: gray;
                -fx-border-radius: 5;
                -fx-background-color: #f9f9f9;
                """);

        Label title = new Label("REQUEST " + request.getId());
        title.setStyle("-fx-font-weight: bold;");

        Label name = new Label("Name: " +
                request.getEmployee().getFirstName() + " " +
                request.getEmployee().getLastName());

        Label phone = new Label("Phone: " + request.getEmployee().getPhone());
        Label email = new Label("Email: " + request.getEmployee().getEmail());

        Button reasonBtn = new Button("Show Reason");
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText(null);
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        ComboBox<LeaveStatus> statusCombo =
                new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));
        statusCombo.setValue(request.getStatus());

        statusCombo.setOnAction(e -> {
            request.setStatus(statusCombo.getValue());
            leaveRequestService.submitRequest(request);
        });

        box.getChildren().addAll(
                title, name, phone, email, reasonBtn, statusCombo
        );

        requestsFlowPane.getChildren().add(box);
    }
}
