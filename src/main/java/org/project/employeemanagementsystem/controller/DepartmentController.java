package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
//test
@Controller
public class DepartmentController implements Initializable {

    @Autowired private DepartmentService departmentService;
    @Autowired private EmployeeService employeeService;

    // --- VIEWS & OVERLAYS ---
    @FXML private VBox tableViewContainer;
    @FXML private VBox formViewContainer;
    @FXML private VBox loadingOverlay; // Loading screen overlay
    @FXML private Label formTitle;

    // --- TABLE ELEMENTS ---
    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, Long> idCol;
    @FXML private TableColumn<Department, String> nameCol;
    @FXML private TableColumn<Department, Integer> employeeCountCol;
    @FXML private TableColumn<Department, Void> actionCol;

    @FXML private TextField searchField;
    @FXML private TextField departmentNameField;
    @FXML private TextArea departmentDescriptionField;

    private ObservableList<Department> departmentList = FXCollections.observableArrayList();
    private FilteredList<Department> filteredData;
    private Department selectedDepartment;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Show loading initially
        loadingOverlay.setVisible(true);
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(false);

        Platform.runLater(this::loadDepartments);

        // Search filter listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    private void loadDepartments() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<List<Department>> task = new Task<>() {
            @Override
            protected List<Department> call() throws Exception {
                return departmentService.getAllDepartments();
            }
        };

        task.setOnSucceeded(event -> {
            List<Department> depts = task.getValue();
            departmentList.setAll(depts);
            filteredData = new FilteredList<>(departmentList, d -> true);
            departmentTable.setItems(filteredData);

            setupTableColumns();
            addActionButtonsToTable();

            applyFilter(searchField.getText());

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

        task.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            Throwable error = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load data: " + error.getMessage());
            styleDialog(alert);
            alert.show();
        });

        new Thread(task).start();
    }

    private void setupTableColumns() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        Map<Long, Long> counts = employeeService.getAllEmployees().stream()
                .collect(Collectors.groupingBy(e -> e.getDepartment().getId(), Collectors.counting()));

        employeeCountCol.setCellValueFactory(cellData -> {
            Department dept = cellData.getValue();
            long count = counts.getOrDefault(dept.getId(), 0L);
            return new javafx.beans.property.SimpleIntegerProperty((int) count).asObject();
        });
    }

    private void applyFilter(String query) {
        if (query == null || query.isEmpty()) filteredData.setPredicate(d -> true);
        else {
            String lower = query.toLowerCase();
            filteredData.setPredicate(d -> d.getName().toLowerCase().contains(lower));
        }
    }

    @FXML
    public void handleAddDepartment() {
        selectedDepartment = null;
        departmentNameField.clear();
        departmentDescriptionField.clear();
        formTitle.setText("New Department");
        tableViewContainer.setVisible(false);
        formViewContainer.setVisible(true);
    }

    @FXML
    public void handleBackToTable() {
        formViewContainer.setVisible(false);
        tableViewContainer.setVisible(true);
    }

    @FXML
    public void handleRefresh() {
        loadDepartments();
    }

    @FXML
    public void handleSaveDepartment() {
        String name = departmentNameField.getText().trim();
        String desc = departmentDescriptionField.getText();

        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Department name cannot be empty!");
            styleDialog(alert);
            alert.show();
            return;
        }

        if (selectedDepartment == null) {
            selectedDepartment = new Department();
        }
        selectedDepartment.setName(name);
        selectedDepartment.setDescription(desc);

        departmentService.saveDepartment(selectedDepartment);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Department saved successfully!");
        styleDialog(alert);
        alert.showAndWait();

        handleRefresh();
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

                btnEdit.setOnAction(event -> {
                    selectedDepartment = getTableView().getItems().get(getIndex());
                    departmentNameField.setText(selectedDepartment.getName());
                    departmentDescriptionField.setText(selectedDepartment.getDescription());
                    formTitle.setText("Edit Department");
                    tableViewContainer.setVisible(false);
                    formViewContainer.setVisible(true);
                });

                btnDelete.setOnAction(event -> {
                    Department dept = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete " + dept.getName() + "?");
                    styleDialog(confirm);
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        departmentService.deleteDepartment(dept);
                        departmentList.remove(dept);

                        Alert info = new Alert(Alert.AlertType.INFORMATION, "Department deleted.");
                        styleDialog(info);
                        info.showAndWait();
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

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
