package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.Employee;
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

    private List<LeaveRequest> allRequests = new ArrayList<>();
    private boolean isPrivileged = false;
    private Employee currentEmployee;

    @FXML
    public void initialize() {

        User currentUser = userSession.getCurrentUser();

        // ✅ ADMIN OR HR HAVE SAME PRIVILEGES
        if (currentUser != null && currentUser.getRole() != null) {
            String roleName = currentUser.getRole().getName();
            isPrivileged =
                    "Admin".equalsIgnoreCase(roleName) ||
                            "HR".equalsIgnoreCase(roleName);
        }

        if (!isPrivileged && currentUser != null) {
            currentEmployee = currentUser.getEmployee();
        }

        // --- Status filter setup
        List<LeaveStatus> statuses = new ArrayList<>();
        statuses.add(null); // All
        statuses.addAll(List.of(LeaveStatus.values()));
        statusFilterCombo.setItems(FXCollections.observableArrayList(statuses));
        statusFilterCombo.setPromptText("All");

        // --- Filtering listeners
        searchField.textProperty().addListener((obs, o, n) -> refreshUIOnly());
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> refreshUIOnly());

        // --- FlowPane layout
        requestsFlowPane.setHgap(15);
        requestsFlowPane.setVgap(15);
        requestsFlowPane.setPadding(new Insets(10));

        // --- Initial load
        loadDataFromDB();
    }

    @FXML
    private void onRefresh() {
        loadDataFromDB();
    }

    private void loadDataFromDB() {

        List<LeaveRequest> fetched = leaveRequestService.getAllRequests();

        // ✅ NON-PRIVILEGED USERS SEE ONLY THEIR OWN REQUESTS
        if (!isPrivileged && currentEmployee != null) {
            Long empId = currentEmployee.getId();
            fetched = fetched.stream()
                    .filter(r -> r.getEmployee() != null
                            && r.getEmployee().getId().equals(empId))
                    .collect(Collectors.toList());
        }

        allRequests = fetched;
        refreshUIOnly();
    }

    private void refreshUIOnly() {
        requestsFlowPane.getChildren().clear();
        if (allRequests == null || allRequests.isEmpty()) return;

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r -> selectedStatus == null || r.getStatus() == selectedStatus)
                .filter(r -> {
                    if (r.getEmployee() == null) return false;
                    String name = (r.getEmployee().getFirstName() + " " +
                            r.getEmployee().getLastName()).toLowerCase();
                    return name.contains(search);
                })
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    private void createRequestCard(LeaveRequest request) {

        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setMinWidth(220);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle(getStatusStyle(request.getStatus()));

        Label title = new Label(
                "REQ #" + request.getId() + " - " + request.getLeaveType().getName()
        );
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label name = new Label(
                request.getEmployee().getFirstName() + " " +
                        request.getEmployee().getLastName()
        );
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label dates = new Label(
                request.getStartDate() + " to " + request.getEndDate()
        );
        dates.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        Button reasonBtn = new Button("View Reason");
        reasonBtn.setMaxWidth(Double.MAX_VALUE);
        reasonBtn.getStyleClass().add("btn-secondary");
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText("Request #" + request.getId());
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        ComboBox<LeaveStatus> statusCombo =
                new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));

        statusCombo.setValue(request.getStatus());
        statusCombo.setDisable(!isPrivileged);
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        if (isPrivileged) {
            statusCombo.setOnAction(e -> {
                LeaveStatus newStatus = statusCombo.getValue();
                request.setStatus(newStatus);
                try {
                    leaveRequestService.updateRequestStatus(request);
                    card.setStyle(getStatusStyle(newStatus));
                } catch (Exception ex) {
                    statusCombo.setValue(request.getStatus());
                    new Alert(Alert.AlertType.ERROR,
                            "Failed to update status").show();
                }
            });
        }

        card.getChildren().addAll(
                title,
                name,
                dates,
                reasonBtn,
                new Separator(),
                new Label("Set Status:"),
                statusCombo
        );

        requestsFlowPane.getChildren().add(card);
    }

    private String getStatusStyle(LeaveStatus status) {
        String base =
                "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);";

        if (status == null) return base + "-fx-background-color: #f9f9f9;";

        return switch (status) {
            case PENDING ->
                    base + "-fx-background-color: #FFF3CD; -fx-border-color: #FFC107;";
            case APPROVED ->
                    base + "-fx-background-color: #DCFCE7; -fx-border-color: #22C55E;";
            case REJECTED ->
                    base + "-fx-background-color: #FEE2E2; -fx-border-color: #EF4444;";
        };
    }
}
