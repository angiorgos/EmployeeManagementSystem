package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class PayrollController implements Initializable {

    // --- Dynamic Settings Variables ---
    private double standardMonthlyHours = 176.0;
    private double overtimeRate = 1.25;
    private double sundayRate = 1.50;
    private double employerInsuranceRate = 0.30;
    private final double employeeInsuranceShareRatio = 0.5; // Fixed ratio (half of employer)

    // --- FXML Elements ---
    @FXML private VBox settingsPanel; // The hidden panel

    // Inputs
    @FXML private TextField txtStandardHours;
    @FXML private TextField txtOvertimeRate;
    @FXML private TextField txtSundayRate;
    @FXML private TextField txtInsuranceRate;

    // Table
    @FXML private TableView<Payment> payrollTable;
    @FXML private TableColumn<Payment, Long> colId;
    @FXML private TableColumn<Payment, String> colName;
    @FXML private TableColumn<Payment, Double> colAmount;
    @FXML private TableColumn<Payment, LocalDate> colDate;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, Void> colActions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialize Inputs with defaults
        txtStandardHours.setText(String.valueOf(standardMonthlyHours));
        txtOvertimeRate.setText(String.valueOf(overtimeRate));
        txtSundayRate.setText(String.valueOf(sundayRate));
        txtInsuranceRate.setText(String.valueOf(employerInsuranceRate));

        // 2. Setup Table
        setupTableColumns();
        loadCalculatedData();
    }

    // --- Settings Logic ---

    @FXML
    public void toggleSettings() {
        // Toggle visibility and "managed" state (so it doesn't take up space when hidden)
        boolean isVisible = settingsPanel.isVisible();
        settingsPanel.setVisible(!isVisible);
        settingsPanel.setManaged(!isVisible);
    }

    @FXML
    public void saveSettings() {
        try {
            // Update variables from TextFields
            standardMonthlyHours = Double.parseDouble(txtStandardHours.getText());
            overtimeRate = Double.parseDouble(txtOvertimeRate.getText());
            sundayRate = Double.parseDouble(txtSundayRate.getText());
            employerInsuranceRate = Double.parseDouble(txtInsuranceRate.getText());

            // Refresh table with new math
            loadCalculatedData();

            // Optional: Auto-hide panel after save
            toggleSettings();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers.");
            alert.show();
        }
    }

    // --- Existing Table & Math Logic ---

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("netPay"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colActions.setCellFactory(getButtonCellFactory());
    }

    private void loadCalculatedData() {
        ObservableList<Payment> data = FXCollections.observableArrayList();

        // Mock Data using CURRENT variables
        data.add(calculatePayroll(1L, "John Doe", 2500.00, 176, 0, 0));
        data.add(calculatePayroll(2L, "Jane Smith", 3200.00, 160, 0, 0)); // Short hours
        data.add(calculatePayroll(3L, "Michael Brown", 1800.00, 176, 10, 5)); // Overtime

        payrollTable.setItems(data);
    }

    private Payment calculatePayroll(Long id, String name, double baseSalary, double hoursWorked, double overtimeHours, double sundayHours) {
        double hourlyPay = baseSalary / standardMonthlyHours;

        double hoursMissed = Math.max(0, standardMonthlyHours - hoursWorked);
        double penalty = hourlyPay * hoursMissed;

        // Use the dynamic variables here
        double overtimePay = hourlyPay * overtimeRate * overtimeHours;
        double sundayPay = hourlyPay * sundayRate * sundayHours;

        double grossPay = baseSalary - penalty + overtimePay + sundayPay;

        // Insurance Math
        double employerInsuranceCost = grossPay * employerInsuranceRate;
        double employeeInsuranceDeduction = employerInsuranceCost * employeeInsuranceShareRatio;

        double netPay = grossPay - employeeInsuranceDeduction;

        return new Payment(id, name, baseSalary, hoursWorked, overtimeHours, sundayHours,
                grossPay, employeeInsuranceDeduction, netPay, LocalDate.now(), "Pending");
    }

    // --- Button Factory (View Details) ---
    private Callback<TableColumn<Payment, Void>, TableCell<Payment, Void>> getButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Payment, Void> call(final TableColumn<Payment, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Details");
                    {
                        btn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 10px;");
                        btn.setOnAction(event -> showPaymentDetails(getTableView().getItems().get(getIndex())));
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                };
            }
        };
    }

    private void showPaymentDetails(Payment p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Details - " + p.getEmployeeName());
        alert.setHeaderText("Salary Breakdown");
        String content = String.format(
                "Base Salary:      $%.2f\n" +
                        "Hours Worked:     %.1f / %.1f\n" +
                        "Overtime (x%.2f):   %.1f hrs\n" +
                        "Sunday (x%.2f):     %.1f hrs\n" +
                        "-----------------------------\n" +
                        "GROSS PAY:        $%.2f\n" +
                        "Insurance (15%%):  -$%.2f\n" +
                        "-----------------------------\n" +
                        "NET PAY:          $%.2f",
                p.getBaseSalary(), p.getHoursWorked(), standardMonthlyHours,
                overtimeRate, p.getOvertimeHours(),
                sundayRate, p.getSundayHours(),
                p.getGrossPay(), p.getDeductions(), p.getNetPay()
        );
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Payment Inner Class ---
    public static class Payment {
        private Long id;
        private String employeeName;
        private Double baseSalary;
        private Double hoursWorked;
        private Double overtimeHours;
        private Double sundayHours;
        private Double grossPay;
        private Double deductions;
        private Double netPay;
        private LocalDate paymentDate;
        private String status;

        public Payment(Long id, String employeeName, Double baseSalary, Double hoursWorked, Double overtimeHours, Double sundayHours,
                       Double grossPay, Double deductions, Double netPay, LocalDate paymentDate, String status) {
            this.id = id;
            this.employeeName = employeeName;
            this.baseSalary = baseSalary;
            this.hoursWorked = hoursWorked;
            this.overtimeHours = overtimeHours;
            this.sundayHours = sundayHours;
            this.grossPay = grossPay;
            this.deductions = deductions;
            this.netPay = netPay;
            this.paymentDate = paymentDate;
            this.status = status;
        }

        public Long getId() { return id; }
        public String getEmployeeName() { return employeeName; }
        public Double getNetPay() { return Math.round(netPay * 100.0) / 100.0; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public String getStatus() { return status; }
        public Double getBaseSalary() { return baseSalary; }
        public Double getHoursWorked() { return hoursWorked; }
        public Double getOvertimeHours() { return overtimeHours; }
        public Double getSundayHours() { return sundayHours; }
        public Double getGrossPay() { return Math.round(grossPay * 100.0) / 100.0; }
        public Double getDeductions() { return Math.round(deductions * 100.0) / 100.0; }
    }
}