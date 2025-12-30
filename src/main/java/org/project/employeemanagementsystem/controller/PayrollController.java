package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.PaymentService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class PayrollController implements Initializable {

    private final PaymentService paymentService;
    private final EmployeeService employeeService;

    // --- State Variables ---
    private double currentOvertimeRate = 1.50;
    private double currentSundayRate = 1.75;
    private double currentInsuranceRate = 0.16;

    public PayrollController(PaymentService paymentService, EmployeeService employeeService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
    }

    // --- FXML Elements ---
    @FXML private Label lblTotalCost;
    @FXML private Label lblPendingCount;
    @FXML private DatePicker monthPicker;
    @FXML private TextField searchField;

    // --- ΤΑ ΚΟΥΜΠΙΑ ΠΟΥ ΕΛΕΙΠΑΝ ---
    @FXML private Button btnGenerate;
    @FXML private Button btnFinalize;
    // ------------------------------

    @FXML private TableView<Payment> payrollTable;
    @FXML private TableColumn<Payment, Long> colId;
    @FXML private TableColumn<Payment, String> colSsn;
    @FXML private TableColumn<Payment, String> colName;
    @FXML private TableColumn<Payment, String> colMonth;
    @FXML private TableColumn<Payment, Double> colGross;
    @FXML private TableColumn<Payment, Double> colDeductions;
    @FXML private TableColumn<Payment, Double> colAmount;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, Void> colActions;

    private ObservableList<Payment> masterData = FXCollections.observableArrayList();
    private FilteredList<Payment> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        monthPicker.setValue(LocalDate.now());

        setupTableColumns();
        loadData();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        monthPicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colSsn.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            return new SimpleStringProperty((emp != null && emp.getSsn() != null) ? emp.getSsn() : "-");
        });

        colName.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            return new SimpleStringProperty(emp != null ? emp.getLastName() + " " + emp.getFirstName() : "Unknown");
        });

        colMonth.setCellValueFactory(new PropertyValueFactory<>("monthYear"));
        colGross.setCellValueFactory(new PropertyValueFactory<>("grossPay"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    if ("PAID".equalsIgnoreCase(item)) setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    else if ("PENDING".equalsIgnoreCase(item)) setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #EF4444;");
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnInfo = new Button("Info");
            private final Button btnBonus = new Button("Bonus");
            private final HBox pane = new HBox(5, btnInfo, btnBonus);

            {
                btnInfo.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                btnBonus.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");

                btnInfo.setOnAction(event -> showPaymentDetails(getTableView().getItems().get(getIndex())));

                btnBonus.setOnAction(event -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        new Alert(Alert.AlertType.WARNING, "Cannot edit a PAID payment!").show();
                    } else {
                        openBonusDialog(p);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        List<Payment> list = paymentService.getAllPayments();
        masterData.setAll(list);
        filteredData = new FilteredList<>(masterData);
        payrollTable.setItems(filteredData);
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        LocalDate selectedDate = monthPicker.getValue();
        String selectedMonthStr = (selectedDate != null) ?
                selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy")) : null;

        filteredData.setPredicate(p -> {
            boolean matchesName = true;
            if (!query.isEmpty()) {
                String fullName = (p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName()).toLowerCase();
                matchesName = fullName.contains(query);
            }
            boolean matchesDate = true;
            if (selectedMonthStr != null) {
                matchesDate = p.getMonthYear().equals(selectedMonthStr);
            }
            return matchesName && matchesDate;
        });

        updateSummaryCards(filteredData);
    }

    private void updateSummaryCards(List<Payment> currentList) {
        if (currentList == null) return;

        double totalCost = currentList.stream().mapToDouble(Payment::getGrossPay).sum();
        long pendingCount = currentList.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getStatus())).count();
        // long paidCount = currentList.stream().filter(p -> "PAID".equalsIgnoreCase(p.getStatus())).count(); // Αν το χρειαστείς

        lblTotalCost.setText(String.format("€ %.2f", totalCost));
        lblPendingCount.setText(String.valueOf(pendingCount));

        // --- BUTTON LOGIC ---
        // 1. Αν η λίστα είναι άδεια -> Δείξε Generate, Κρύψε Finalize
        if (currentList.isEmpty()) {
            if (btnGenerate != null) { btnGenerate.setVisible(true); btnGenerate.setManaged(true); }
            if (btnFinalize != null) { btnFinalize.setVisible(false); btnFinalize.setManaged(false); }
        }
        // 2. Αν υπάρχουν PENDING -> Κρύψε Generate, Δείξε Finalize
        else if (pendingCount > 0) {
            if (btnGenerate != null) { btnGenerate.setVisible(false); btnGenerate.setManaged(false); }
            if (btnFinalize != null) { btnFinalize.setVisible(true); btnFinalize.setManaged(true); }
        }
        // 3. Αν όλα είναι PAID -> Κρύψε και τα δύο
        else {
            if (btnGenerate != null) { btnGenerate.setVisible(false); btnGenerate.setManaged(false); }
            if (btnFinalize != null) { btnFinalize.setVisible(false); btnFinalize.setManaged(false); }
        }
    }

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a month first!").show();
            return;
        }

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;
            paymentService.calculateAndSavePayroll(emp, 176.0, 0.0, 0.0,
                    currentOvertimeRate, currentSundayRate, currentInsuranceRate);
            count++;
        }

        loadData();
        new Alert(Alert.AlertType.INFORMATION, "Generated payroll for " + count + " employees!").show();
    }

    @FXML
    public void finalizePayments() {
        List<Payment> pendingPayments = filteredData.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        if (pendingPayments.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No PENDING payments found.").show();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark " + pendingPayments.size() + " payments as PAID?\nThis action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (Payment p : pendingPayments) {
                p.setStatus("PAID");
                // Χρησιμοποιούμε την updateBonus σαν save (hack) ή φτιάξε μέθοδο save() σκέτη
                paymentService.updateBonus(p, (p.getBonus()!=null?p.getBonus():0), currentInsuranceRate);
            }
            payrollTable.refresh();
            updateSummaryCards(filteredData);
            new Alert(Alert.AlertType.INFORMATION, "Payments Finalized!").show();
        }
    }

    @FXML
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configuration");
        dialog.setHeaderText("Adjust Payroll Rates");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField otField = new TextField(String.valueOf(currentOvertimeRate));
        TextField sunField = new TextField(String.valueOf(currentSundayRate));
        TextField insField = new TextField(String.valueOf(currentInsuranceRate));

        grid.addRow(0, new Label("Overtime (x):"), otField);
        grid.addRow(1, new Label("Sunday (x):"), sunField);
        grid.addRow(2, new Label("Insurance (%):"), insField);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtn) {
                try {
                    currentOvertimeRate = Double.parseDouble(otField.getText());
                    currentSundayRate = Double.parseDouble(sunField.getText());
                    currentInsuranceRate = Double.parseDouble(insField.getText());
                } catch (Exception e) { /* Ignore */ }
            }
        });
    }

    private void openBonusDialog(Payment payment) {
        TextInputDialog dialog = new TextInputDialog(payment.getBonus() != null ? payment.getBonus().toString() : "0.0");
        dialog.setTitle("Add Bonus");
        dialog.setHeaderText("Bonus for: " + payment.getEmployee().getLastName());
        dialog.setContentText("Amount (€):");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double newBonus = Double.parseDouble(amountStr);
                paymentService.updateBonus(payment, newBonus, currentInsuranceRate);
                loadData();
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Invalid amount!").show();
            }
        });
    }

    private void showPaymentDetails(Payment p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());

        String ssn = (p.getEmployee().getSsn() != null) ? p.getEmployee().getSsn() : "-";
        double bonus = (p.getBonus() != null) ? p.getBonus() : 0.0;

        String content = String.format(
                "SSN:              %s\n" +
                        "Month:            %s\n" +
                        "Status:           %s\n" +
                        "-----------------------------\n" +
                        "Base Salary:      €%.2f\n" +
                        "Bonus:            €%.2f\n" +
                        "GROSS PAY:        €%.2f\n" +
                        "Deductions:      -€%.2f\n" +
                        "-----------------------------\n" +
                        "NET PAY:          €%.2f",
                ssn, p.getMonthYear(), p.getStatus(),
                p.getBaseSalary(), bonus,
                p.getGrossPay(), p.getDeductions(), p.getAmount()
        );
        alert.setContentText(content);
        alert.showAndWait();
    }
}