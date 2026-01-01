package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LeavesController2 {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private UserSession userSession;

    @FXML private FlowPane requestsFlowPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<LeaveStatus> statusFilterCombo;
    @FXML private Button refreshBtn;

    private List<LeaveRequest> allRequests;
    private boolean isAdmin;

    @FXML
    public void initialize() {
        // --- Determine if current user is admin
        User currentUser = userSession.getCurrentUser();
        isAdmin = currentUser != null
                && currentUser.getRole() != null
                && "ROLE_ADMIN".equals(currentUser.getRole().getName());

        // --- Setup Status Filter
        List<LeaveStatus> statuses = new ArrayList<>();
        statuses.add(null); // "All"
        statuses.addAll(List.of(LeaveStatus.values()));
        statusFilterCombo.setItems(FXCollections.observableArrayList(statuses));
        statusFilterCombo.setPromptText("All");

        // --- Setup Listeners for Filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshUIOnly());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshUIOnly());

        // --- Setup FlowPane
        requestsFlowPane.setHgap(15);
        requestsFlowPane.setVgap(15);
        requestsFlowPane.setPadding(new Insets(10));

        // --- Initial Load
        loadDataFromDB();
    }

    @FXML
    private void onRefresh() {
        loadDataFromDB();
    }

    private void loadDataFromDB() {
        allRequests = leaveRequestService.getAllRequests();
        refreshUIOnly();
    }

    private void refreshUIOnly() {
        requestsFlowPane.getChildren().clear();
        if (allRequests == null) return;

        String search = (searchField.getText() != null) ? searchField.getText().toLowerCase().trim() : "";
        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r -> (selectedStatus == null || r.getStatus() == selectedStatus))
                .filter(r -> {
                    String fullName = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).toLowerCase();
                    return fullName.contains(search);
                })
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    private void createRequestCard(LeaveRequest request) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setSpacing(8);
        card.setMinWidth(220);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle(getStatusStyle(request.getStatus()));

        Label title = new Label("REQ #" + request.getId() + " - " + request.getLeaveType().getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label name = new Label(request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label dates = new Label(request.getStartDate() + " to " + request.getEndDate());
        dates.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        Button reasonBtn = new Button("View Reason");
        reasonBtn.setMaxWidth(Double.MAX_VALUE);
        reasonBtn.getStyleClass().add("btn-secondary");
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText("Reason for Request #" + request.getId());
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        ComboBox<LeaveStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));
        statusCombo.setValue(request.getStatus());
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        // --- Disable ComboBox if user is not admin
        statusCombo.setDisable(!isAdmin);

        statusCombo.setOnAction(e -> {
            if (!isAdmin) return; // safety check

            LeaveStatus newStatus = statusCombo.getValue();
            request.setStatus(newStatus);

            try {
                leaveRequestService.updateRequestStatus(request);
                card.setStyle(getStatusStyle(newStatus));
            } catch (Exception ex) {
                ex.printStackTrace();
                statusCombo.setValue(request.getStatus());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Update failed: " + ex.getMessage());
                alert.show();
            }
        });

        card.getChildren().addAll(title, name, dates, reasonBtn, new Separator(), new Label("Set Status:"), statusCombo);
        requestsFlowPane.getChildren().add(card);
    }

    private String getStatusStyle(LeaveStatus status) {
        String base = "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); -fx-padding: 10;";
        if (status == null) return base + " -fx-background-color: #f9f9f9;";
        switch (status) {
            case PENDING:  return base + " -fx-background-color: #FFF3CD; -fx-border-color: #FFC107;";
            case APPROVED: return base + " -fx-background-color: #DCFCE7; -fx-border-color: #22C55E;";
            case REJECTED: return base + " -fx-background-color: #FEE2E2; -fx-border-color: #EF4444;";
            default:       return base + " -fx-background-color: #f9f9f9;";
        }
    }
}
