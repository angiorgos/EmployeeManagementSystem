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
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.service.AttendanceService; // <-- ΝΕΟ IMPORT
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.PaymentService;
import org.project.employeemanagementsystem.service.SystemSettingService;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class PayrollController implements Initializable {

    private final PaymentService paymentService;
    private final EmployeeService employeeService;
    private final SystemSettingService settingService;
    private final AttendanceService attendanceService; // <-- ΝΕΟ FIELD

    // --- KEYS SETTINGS ---
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";
    private static final String KEY_TAX = "payroll.total_tax_rate";
    private static final String KEY_EMPLOYER_SHARE = "payroll.employer_share";
    private static final String KEY_CURRENCY = "company.currency";

    // Προσθέτουμε το attendanceService στον Constructor
    public PayrollController(PaymentService paymentService, EmployeeService employeeService,
                             SystemSettingService settingService, AttendanceService attendanceService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
        this.settingService = settingService;
        this.attendanceService = attendanceService;
    }

    // --- FXML FIELDS ---
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

    // ================= ACTIONS =================

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a month first!");
            return;
        }

        // 1. Διάβασμα Ρυθμίσεων
        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double otRate   = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate  = settingService.getDouble(KEY_SUNDAY, 1.75);
        double taxRate  = settingService.getDouble(KEY_TAX, 0.40);
        double empSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;

            // --- ΑΥΤΟΜΑΤΟΣ ΥΠΟΛΟΓΙΣΜΟΣ ΑΠΟ ATTENDANCE ---

            // Α. Βρίσκουμε τις συνολικές ώρες που δούλεψε
            double realHours = attendanceService.calculateTotalHoursWorked(emp, selectedDate);

            // Β. Βρίσκουμε τις ώρες Κυριακής
            double sundayHours = attendanceService.calculateSundayHours(emp, selectedDate);

            // Γ. Υπολογίζουμε Υπερωρίες (Αν δούλεψε παραπάνω από το stdHours)
            double overtimeHours = 0.0;
            if (realHours > stdHours) {
                overtimeHours = realHours - stdHours;
            }

            // 3. Αποθήκευση Μισθοδοσίας
            paymentService.calculateAndSavePayroll(
                    emp,
                    selectedDate,
                    stdHours,       // Πρότυπες ώρες (για διαίρεση ωρομισθίου)
                    overtimeHours,  // Πραγματικές υπερωρίες
                    sundayHours,    // Πραγματικές ώρες Κυριακής
                    otRate, sunRate, taxRate, empSplit
            );
            count++;
        }

        loadData();
        String monthStr = selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        showSimpleAlert(Alert.AlertType.INFORMATION, "Success", "Generated payroll for " + count + " employees (" + monthStr + ")");
    }

    @FXML
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Payroll Configuration");
        styleAlert(dialog);

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10); grid.setPadding(new Insets(20));

        // Load Current
        TextField hoursField = new TextField(String.valueOf(settingService.getDouble(KEY_WORK_HOURS, 176.0)));
        TextField otField = new TextField(String.valueOf(settingService.getDouble(KEY_OVERTIME, 1.50)));
        TextField sunField = new TextField(String.valueOf(settingService.getDouble(KEY_SUNDAY, 1.75))); // Sunday Rate Field
        TextField taxField = new TextField(String.valueOf(settingService.getDouble(KEY_TAX, 0.40)));
        TextField splitField = new TextField(String.valueOf(settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60)));
        TextField currencyField = new TextField(settingService.getString(KEY_CURRENCY, "€"));

        grid.addRow(0, new Label("Standard Monthly Hours:"), hoursField);
        grid.addRow(1, new Label("Overtime Rate (x):"), otField);
        grid.addRow(2, new Label("Sunday Rate (x):"), sunField);
        grid.addRow(3, new Label("Total Tax Rate (0.xx):"), taxField);
        grid.addRow(4, new Label("Employer Share (0.xx):"), splitField);
        grid.addRow(5, new Label("Currency:"), currencyField);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtnType) {
                try {
                    settingService.save(KEY_WORK_HOURS, Double.parseDouble(hoursField.getText()));
                    settingService.save(KEY_OVERTIME, Double.parseDouble(otField.getText()));
                    settingService.save(KEY_SUNDAY, Double.parseDouble(sunField.getText()));
                    settingService.save(KEY_TAX, Double.parseDouble(taxField.getText()));
                    settingService.save(KEY_EMPLOYER_SHARE, Double.parseDouble(splitField.getText()));
                    settingService.save(KEY_CURRENCY, currencyField.getText());
                    showSimpleAlert(Alert.AlertType.INFORMATION, "Saved", "Settings updated!");
                } catch (Exception e) {
                    showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid input!");
                }
            }
        });
    }

    // --- ΒΟΗΘΗΤΙΚΕΣ (BONUS, EXPORT, FINALIZE) ---

    private void openBonusDialog(Payment payment) {
        String currentBonus = (payment.getBonus() != null) ? payment.getBonus().toString() : "0.0";
        TextInputDialog dialog = new TextInputDialog(currentBonus);
        dialog.setTitle("Add Bonus");
        dialog.setHeaderText("Bonus for: " + payment.getEmployee().getLastName());
        styleAlert(dialog);

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double newBonus = Double.parseDouble(amountStr);
                double taxRate = settingService.getDouble(KEY_TAX, 0.40);
                double empShare = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);
                paymentService.updateBonus(payment, newBonus, taxRate, empShare);
                loadData();
            } catch (NumberFormatException e) {
                showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid bonus amount!");
            }
        });
    }

    @FXML
    public void exportToExcel() {
        List<Payment> rows = payrollTable.getItems();
        if (rows.isEmpty()) {
            showSimpleAlert(Alert.AlertType.WARNING, "No Data", "No payroll data to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payroll Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Payroll_" + LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(payrollTable.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Payroll Data");
                String currency = settingService.getString(KEY_CURRENCY, "€");

                String[] columns = {
                        "ID", "SSN", "Name", "Month",
                        "Base Salary", "Overtime Hrs", "Sunday Hrs", "Bonus", "Gross Pay",
                        "Deductions", "Net Pay (" + currency + ")", "Status"
                };

                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                for (Payment p : rows) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(p.getId());
                    row.createCell(1).setCellValue(p.getEmployee().getSsn() != null ? p.getEmployee().getSsn() : "-");
                    row.createCell(2).setCellValue(p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName());
                    row.createCell(3).setCellValue(p.getMonthYear());
                    row.createCell(4).setCellValue(p.getBaseSalary());
                    row.createCell(5).setCellValue(p.getOvertimeHours());
                    row.createCell(6).setCellValue(p.getSundayHours());
                    row.createCell(7).setCellValue(p.getBonus() != null ? p.getBonus() : 0.0);
                    row.createCell(8).setCellValue(p.getGrossPay());
                    row.createCell(9).setCellValue(p.getDeductions());
                    row.createCell(10).setCellValue(p.getAmount());
                    row.createCell(11).setCellValue(p.getStatus());
                }
                for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
                showSimpleAlert(Alert.AlertType.INFORMATION, "Success", "Export successful!");
            } catch (IOException e) {
                showSimpleAlert(Alert.AlertType.ERROR, "Export Error", e.getMessage());
            }
        }
    }

    @FXML
    public void finalizePayments() {
        List<Payment> pending = filteredData.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        if (pending.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Mark " + pending.size() + " payments as PAID?");
        styleAlert(alert);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            for (Payment p : pending) {
                p.setStatus("PAID");
                paymentService.updatePaymentStatus(p, "PAID");
            }
            payrollTable.refresh();
            updateSummaryCards(filteredData);
        }
    }

    // --- UI SETUP & LOADERS ---

    private void showPaymentDetails(Payment p) {
        String currency = settingService.getString(KEY_CURRENCY, "€");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Analysis");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());
        styleAlert(alert);

        double deductions = (p.getDeductions() != null) ? p.getDeductions() : 0.0;
        double employerCost = (p.getEmployerTax() != null) ? p.getEmployerTax() : 0.0;
        double bonus = (p.getBonus() != null) ? p.getBonus() : 0.0;
        String ssn = (p.getEmployee().getSsn() != null) ? p.getEmployee().getSsn() : "-";

        String content = String.format(
                "SSN: %s\nMonth: %s\nStatus: %s\n" +
                        "Work Hours: %.1f | Overtime: %.1f | Sunday: %.1f\n" + // Προσθήκη ωρών στην ανάλυση
                        "-----------------------------\n" +
                        "Base Salary: %s%.2f\nBonus: %s%.2f\nGROSS PAY: %s%.2f\n" +
                        "-----------------------------\n" +
                        "Deductions: -%s%.2f\nEmployer Cost: %s%.2f\n" +
                        "-----------------------------\n" +
                        "NET PAY: %s%.2f",
                ssn, p.getMonthYear(), p.getStatus(),
                p.getHoursWorked(), p.getOvertimeHours(), p.getSundayHours(),
                currency, p.getBaseSalary(), currency, bonus, currency, p.getGrossPay(),
                currency, deductions, currency, employerCost,
                currency, p.getAmount()
        );
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSsn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEmployee().getSsn() != null ? cell.getValue().getEmployee().getSsn() : "-"));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEmployee().getLastName() + " " + cell.getValue().getEmployee().getFirstName()));
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
                    else setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnInfo = new Button("Info");
            private final Button btnBonus = new Button("Bonus");
            private final HBox pane = new HBox(5, btnInfo, btnBonus);
            {
                btnInfo.getStyleClass().add("table-btn"); btnInfo.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");
                btnBonus.getStyleClass().add("table-btn"); btnBonus.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white;");
                btnInfo.setOnAction(e -> showPaymentDetails(getTableView().getItems().get(getIndex())));
                btnBonus.setOnAction(e -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    if ("PAID".equalsIgnoreCase(p.getStatus())) showSimpleAlert(Alert.AlertType.WARNING, "Locked", "Already Paid!");
                    else openBonusDialog(p);
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
        masterData.setAll(paymentService.getAllPayments());
        filteredData = new FilteredList<>(masterData);
        payrollTable.setItems(filteredData);
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedMonth = (monthPicker.getValue() != null) ? monthPicker.getValue().format(DateTimeFormatter.ofPattern("MM/yyyy")) : null;

        filteredData.setPredicate(p -> {
            boolean matchesName = query.isEmpty() || (p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName()).toLowerCase().contains(query);
            boolean matchesDate = selectedMonth == null || p.getMonthYear().equals(selectedMonth);
            return matchesName && matchesDate;
        });
        updateSummaryCards(filteredData);
    }

    private void updateSummaryCards(List<Payment> list) {
        long pending = list.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getStatus())).count();
        lblPendingCount.setText(String.valueOf(pending));
        setButtonsVisible(list.isEmpty(), pending > 0);
    }

    private void setButtonsVisible(boolean generate, boolean finalize) {
        if (btnGenerate != null) { btnGenerate.setVisible(generate); btnGenerate.setManaged(generate); }
        if (btnFinalize != null) { btnFinalize.setVisible(finalize); btnFinalize.setManaged(finalize); }
    }

    private void showSimpleAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.show();
    }

    private void styleAlert(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
        } catch (Exception e) { /* ignore */ }
    }
}