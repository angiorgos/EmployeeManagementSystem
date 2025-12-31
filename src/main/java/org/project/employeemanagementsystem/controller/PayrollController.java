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
import org.project.employeemanagementsystem.service.SystemSettingService;
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
    private final SystemSettingService settingService;

    // Κλειδιά για τη βάση δεδομένων
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";
    private static final String KEY_TAX = "payroll.total_tax_rate";
    private static final String KEY_EMPLOYER_SHARE = "payroll.employer_share";

    public PayrollController(PaymentService paymentService, EmployeeService employeeService, SystemSettingService settingService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
        this.settingService = settingService;
    }

    @FXML private Label lblTotalCost;
    @FXML private Label lblPendingCount;
    @FXML private DatePicker monthPicker;
    @FXML private TextField searchField;
    @FXML private Button btnGenerate;
    @FXML private Button btnFinalize;

    @FXML private TableView<Payment> payrollTable;
    @FXML private TableColumn<Payment, Long> colId;
    @FXML private TableColumn<Payment, String> colSsn;
    @FXML private TableColumn<Payment, String> colName;
    @FXML private TableColumn<Payment, String> colMonth;
    @FXML private TableColumn<Payment, Double> colGross;
    @FXML private TableColumn<Payment, Double> colDeductions;
    @FXML private TableColumn<Payment, Double> colEmployerTax;
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

    // --- CSS Styling Method ---
    private void styleAlert(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        URL cssResource = getClass().getResource("/theme.css");
        if (cssResource != null) {
            dialogPane.getStylesheets().add(cssResource.toExternalForm());
            dialogPane.getStyleClass().add("my-dialog");
        }
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
        colEmployerTax.setCellValueFactory(new PropertyValueFactory<>("employerTax"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setText(null); setStyle(""); }
                else {
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
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot edit a PAID payment!");
                        styleAlert(alert);
                        alert.show();
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
        String selectedMonthStr = (selectedDate != null) ? selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy")) : null;

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
        double totalCost = currentList.stream().mapToDouble(Payment::getAmount).sum();
        long pendingCount = currentList.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getStatus())).count();

        lblTotalCost.setText(String.format("€ %.2f", totalCost));
        lblPendingCount.setText(String.valueOf(pendingCount));

        if (currentList.isEmpty()) { setButtonsVisible(true, false); }
        else if (pendingCount > 0) { setButtonsVisible(false, true); }
        else { setButtonsVisible(false, false); }
    }

    private void setButtonsVisible(boolean generate, boolean finalizeBtn) {
        if (btnGenerate != null) { btnGenerate.setVisible(generate); btnGenerate.setManaged(generate); }
        if (btnFinalize != null) { btnFinalize.setVisible(finalizeBtn); btnFinalize.setManaged(finalizeBtn); }
    }

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a month first!");
            styleAlert(alert);
            alert.show();
            return;
        }

        // Φόρτωση ρυθμίσεων από τη βάση
        double otRate = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate = settingService.getDouble(KEY_SUNDAY, 1.75);
        double taxRate = settingService.getDouble(KEY_TAX, 0.40);
        double empSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;
            paymentService.calculateAndSavePayroll(emp, 176.0, 0.0, 0.0, otRate, sunRate, taxRate, empSplit);
            count++;
        }

        loadData();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Generated payroll for " + count + " employees!");
        styleAlert(alert);
        alert.show();
    }

    @FXML
    public void finalizePayments() {
        List<Payment> pendingPayments = filteredData.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        if (pendingPayments.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Mark " + pendingPayments.size() + " payments as PAID?");
        styleAlert(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (Payment p : pendingPayments) {
                p.setStatus("PAID");
                paymentService.updateBonus(p, (p.getBonus()!=null?p.getBonus():0), 0.0);
            }
            payrollTable.refresh();
            updateSummaryCards(filteredData);
        }
    }

    @FXML
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configuration");
        dialog.setHeaderText("Payroll & Tax Settings");
        styleAlert(dialog); // Φορτώνει το CSS

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = ButtonType.CANCEL; // Κρατάμε το default Cancel

        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, cancelBtnType);

        // --- ΕΔΩ ΕΙΝΑΙ Η ΜΑΓΕΙΑ ΓΙΑ ΤΑ ΚΟΥΜΠΙΑ ---
        // Βρίσκουμε τα κουμπιά μέσα στο Dialog και τους δίνουμε CSS κλάσεις
        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        btnSave.getStyleClass().add("btn-primary");

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(cancelBtnType);
        btnCancel.getStyleClass().add("btn-secondary");
        // ----------------------------------------

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        double currentOt = settingService.getDouble(KEY_OVERTIME, 1.50);
        double currentSun = settingService.getDouble(KEY_SUNDAY, 1.75);
        double currentTax = settingService.getDouble(KEY_TAX, 0.40);
        double currentSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        TextField otField = new TextField(String.valueOf(currentOt));
        TextField sunField = new TextField(String.valueOf(currentSun));
        TextField taxRateField = new TextField(String.valueOf(currentTax));
        TextField empSplitField = new TextField(String.valueOf(currentSplit));

        // Styling στα TextFields για να είναι πιο όμορφα
        String fieldStyle = "-fx-background-radius: 4; -fx-border-color: #D1D5DB; -fx-border-radius: 4;";
        otField.setStyle(fieldStyle);
        sunField.setStyle(fieldStyle);
        taxRateField.setStyle(fieldStyle);
        empSplitField.setStyle(fieldStyle);

        grid.addRow(0, new Label("Overtime Rate (x):"), otField);
        grid.addRow(1, new Label("Sunday Rate (x):"), sunField);
        grid.addRow(2, new Label("Total Tax Rate (0.xx):"), taxRateField);
        grid.addRow(3, new Label("Employer Split (0.xx):"), empSplitField);

        Label hint = new Label("(e.g. 0.60 means Employer pays 60% of tax)");
        hint.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        grid.add(hint, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtnType) {
                try {
                    settingService.setDouble(KEY_OVERTIME, Double.parseDouble(otField.getText()));
                    settingService.setDouble(KEY_SUNDAY, Double.parseDouble(sunField.getText()));
                    settingService.setDouble(KEY_TAX, Double.parseDouble(taxRateField.getText()));
                    settingService.setDouble(KEY_EMPLOYER_SHARE, Double.parseDouble(empSplitField.getText()));

                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Settings Saved!");
                    styleAlert(success);
                    success.show();
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Invalid numbers!");
                    styleAlert(error);
                    error.show();
                }
            }
        });
    }

    private void openBonusDialog(Payment payment) {
        TextInputDialog dialog = new TextInputDialog(payment.getBonus() != null ? payment.getBonus().toString() : "0.0");
        dialog.setTitle("Add Bonus");
        dialog.setHeaderText("Bonus for: " + payment.getEmployee().getLastName());
        styleAlert(dialog);

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double newBonus = Double.parseDouble(amountStr);
                paymentService.updateBonus(payment, newBonus, 0.16);
                loadData();
            } catch (NumberFormatException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Invalid amount!");
                styleAlert(error);
                error.show();
            }
        });
    }

    // --- ΕΔΩ ΕΙΝΑΙ Η ΑΛΛΑΓΗ ΣΤΟ STRING FORMAT ---
    private void showPaymentDetails(Payment p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Analysis");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());
        styleAlert(alert);

        double empTax = (p.getDeductions() != null) ? p.getDeductions() : 0.0;
        double bossTax = (p.getEmployerTax() != null) ? p.getEmployerTax() : 0.0;
        double totalStateTax = empTax + bossTax;
        double bonus = (p.getBonus() != null) ? p.getBonus() : 0.0;
        String ssn = (p.getEmployee().getSsn() != null) ? p.getEmployee().getSsn() : "-";

        String content = String.format(
                "SSN:              %s\n" +
                        "Month:            %s\n" +
                        "Status:           %s\n" +
                        "-----------------------------\n" +
                        "Base Salary:      €%.2f\n" +
                        "Bonus:            €%.2f\n" +
                        "GROSS PAY:        €%.2f\n" +
                        "-----------------------------\n" +
                        "TAX BREAKDOWN:\n" +
                        "  Total Tax:      €%.2f\n" +
                        "  - Employee:    -€%.2f (Deducted)\n" +
                        "  - Employer:     €%.2f (Company Cost)\n" +
                        "-----------------------------\n" +
                        "NET PAY:          €%.2f",
                ssn,
                p.getMonthYear(),
                p.getStatus(),
                p.getBaseSalary(),
                bonus,
                p.getGrossPay(),
                totalStateTax,
                empTax,
                bossTax,
                p.getAmount()
        );
        alert.setContentText(content);
        alert.showAndWait();
    }
}