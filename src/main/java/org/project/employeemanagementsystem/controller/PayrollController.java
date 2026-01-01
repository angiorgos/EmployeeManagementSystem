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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class PayrollController implements Initializable {

    // --- SERVICES ---
    private final PaymentService paymentService;
    private final EmployeeService employeeService;
    private final SystemSettingService settingService;

    // --- KEYS ΓΙΑ ΤΗ ΒΑΣΗ ΔΕΔΟΜΕΝΩΝ ---
    // Μισθοδοσία
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";
    private static final String KEY_NIGHT = "payroll.night_rate";      // ΝΕΟ
    private static final String KEY_HOLIDAY = "payroll.holiday_rate";  // ΝΕΟ

    // Φόροι
    private static final String KEY_TAX = "payroll.total_tax_rate";
    private static final String KEY_EMPLOYER_SHARE = "payroll.employer_share";

    // Εταιρικά
    private static final String KEY_COMPANY_NAME = "company.name";     // ΝΕΟ
    private static final String KEY_CURRENCY = "company.currency";     // ΝΕΟ

    public PayrollController(PaymentService paymentService, EmployeeService employeeService, SystemSettingService settingService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
        this.settingService = settingService;
    }

    // --- FXML ELEMENTS ---
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

    // --- DATA ---
    private ObservableList<Payment> masterData = FXCollections.observableArrayList();
    private FilteredList<Payment> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        monthPicker.setValue(LocalDate.now());
        setupTableColumns();
        loadData(); // Φόρτωμα δεδομένων κατά την εκκίνηση

        // Listeners για φίλτρα (Αναζήτηση & Ημερομηνία)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        monthPicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ============================================================
    // MAIN ACTIONS (GENERATE, SETTINGS, EXPORT)
    // ============================================================

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a month first!");
            return;
        }

        // 1. Ανάκτηση ΟΛΩΝ των ρυθμίσεων από τη βάση (με defaults)
        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double otRate   = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate  = settingService.getDouble(KEY_SUNDAY, 1.75);
        double taxRate  = settingService.getDouble(KEY_TAX, 0.40);
        double empSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        // Σημείωση: Αν το PaymentService σου υποστηρίζει Night/Holiday, τράβηξε τα και αυτά:
        // double nightRate = settingService.getDouble(KEY_NIGHT, 1.25);
        // double holidayRate = settingService.getDouble(KEY_HOLIDAY, 2.00);

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;

            // Κλήση του Service για υπολογισμό
            // Προσοχή: Αν ενημερώσεις το PaymentService να δέχεται Night/Holiday, πέρνα τα εδώ.
            paymentService.calculateAndSavePayroll(
                    emp, selectedDate, stdHours,
                    0.0, 0.0, // Overtime Hours, Sunday Hours (αρχικά 0)
                    otRate, sunRate, taxRate, empSplit
            );
            count++;
        }

        loadData(); // Ανανέωση πίνακα
        String monthStr = selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        showSimpleAlert(Alert.AlertType.INFORMATION, "Success",
                "Generated payroll for " + count + " employees (" + monthStr + ")");
    }

    @FXML
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configuration");
        dialog.setHeaderText("System & Payroll Settings");
        styleAlert(dialog);

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        // Styling Buttons
        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10); grid.setPadding(new Insets(20));

        // 1. ΑΝΑΚΤΗΣΗ ΤΡΕΧΟΥΣΩΝ ΤΙΜΩΝ
        // Rates
        double curHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double curOt = settingService.getDouble(KEY_OVERTIME, 1.50);
        double curSun = settingService.getDouble(KEY_SUNDAY, 1.75);
        double curNight = settingService.getDouble(KEY_NIGHT, 1.25);
        double curHoliday = settingService.getDouble(KEY_HOLIDAY, 2.00);
        double curTax = settingService.getDouble(KEY_TAX, 0.40);
        double curSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        // Strings
        String curCompany = settingService.getString(KEY_COMPANY_NAME, "My Company");
        String curCurrency = settingService.getString(KEY_CURRENCY, "€");

        // 2. ΔΗΜΙΟΥΡΓΙΑ ΠΕΔΙΩΝ (UI)
        TextField hoursField = new TextField(String.valueOf(curHours));
        TextField otField = new TextField(String.valueOf(curOt));
        TextField sunField = new TextField(String.valueOf(curSun));
        TextField nightField = new TextField(String.valueOf(curNight));
        TextField holidayField = new TextField(String.valueOf(curHoliday));
        TextField taxField = new TextField(String.valueOf(curTax));
        TextField splitField = new TextField(String.valueOf(curSplit));

        TextField companyField = new TextField(curCompany);
        TextField currencyField = new TextField(curCurrency);

        // Styling Fields
        String fieldStyle = "-fx-background-radius: 4; -fx-border-color: #D1D5DB; -fx-border-radius: 4;";
        hoursField.setStyle(fieldStyle); otField.setStyle(fieldStyle); sunField.setStyle(fieldStyle);
        nightField.setStyle(fieldStyle); holidayField.setStyle(fieldStyle);
        taxField.setStyle(fieldStyle); splitField.setStyle(fieldStyle);
        companyField.setStyle(fieldStyle); currencyField.setStyle(fieldStyle);

        // 3. ΤΟΠΟΘΕΤΗΣΗ ΣΤΟ GRID
        // Section: Company Info
        grid.add(new Label("Company Info"), 0, 0);
        grid.addRow(1, new Label("Company Name:"), companyField);
        grid.addRow(2, new Label("Currency Symbol:"), currencyField);

        grid.add(new Separator(), 0, 3, 2, 1);

        // Section: Payroll Rates
        grid.add(new Label("Payroll Rates"), 0, 4);
        grid.addRow(5, new Label("Std Monthly Hours:"), hoursField);
        grid.addRow(6, new Label("Overtime Rate (x):"), otField);
        grid.addRow(7, new Label("Sunday Rate (x):"), sunField);
        grid.addRow(8, new Label("Night Shift Rate (x):"), nightField);
        grid.addRow(9, new Label("Holiday Rate (x):"), holidayField);

        grid.add(new Separator(), 0, 10, 2, 1);

        // Section: Taxes
        grid.add(new Label("Taxes & Contributions"), 0, 11);
        grid.addRow(12, new Label("Total Tax Rate (0.xx):"), taxField);
        grid.addRow(13, new Label("Employer Split (0.xx):"), splitField);
        grid.add(new Label("(e.g. 0.60 means Employer pays 60%)"), 1, 14);

        dialog.getDialogPane().setContent(grid);

        // 4. ΑΠΟΘΗΚΕΥΣΗ (SAVE)
        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtnType) {
                try {
                    // Save Numbers (χρησιμοποιούμε την έξυπνη save που δέχεται Double)
                    settingService.save(KEY_WORK_HOURS, Double.parseDouble(hoursField.getText()));
                    settingService.save(KEY_OVERTIME, Double.parseDouble(otField.getText()));
                    settingService.save(KEY_SUNDAY, Double.parseDouble(sunField.getText()));
                    settingService.save(KEY_NIGHT, Double.parseDouble(nightField.getText()));
                    settingService.save(KEY_HOLIDAY, Double.parseDouble(holidayField.getText()));
                    settingService.save(KEY_TAX, Double.parseDouble(taxField.getText()));
                    settingService.save(KEY_EMPLOYER_SHARE, Double.parseDouble(splitField.getText()));

                    // Save Strings
                    settingService.save(KEY_COMPANY_NAME, companyField.getText());
                    settingService.save(KEY_CURRENCY, currencyField.getText());

                    showSimpleAlert(Alert.AlertType.INFORMATION, "Saved", "Settings updated successfully!");
                } catch (Exception e) {
                    showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid inputs: " + e.getMessage());
                }
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

                // Ανάκτηση νομίσματος για τον τίτλο
                String currency = settingService.getString(KEY_CURRENCY, "€");

                String[] columns = {
                        "ID", "SSN", "Name", "Month",
                        "Base Salary (" + currency + ")", "Bonus", "Gross Pay",
                        "Deductions", "Employer Cost",
                        "Net Pay (" + currency + ")", "Status"
                };

                // Δημιουργία Header
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

                // Γέμισμα Δεδομένων
                int rowNum = 1;
                for (Payment p : rows) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(p.getId());
                    row.createCell(1).setCellValue(p.getEmployee().getSsn() != null ? p.getEmployee().getSsn() : "-");
                    row.createCell(2).setCellValue(p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName());
                    row.createCell(3).setCellValue(p.getMonthYear());
                    row.createCell(4).setCellValue(p.getBaseSalary() != null ? p.getBaseSalary() : 0.0);
                    row.createCell(5).setCellValue(p.getBonus() != null ? p.getBonus() : 0.0);
                    row.createCell(6).setCellValue(p.getGrossPay());
                    row.createCell(7).setCellValue(p.getDeductions());
                    row.createCell(8).setCellValue(p.getEmployerTax());
                    row.createCell(9).setCellValue(p.getAmount());
                    row.createCell(10).setCellValue(p.getStatus());
                }

                for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
                showSimpleAlert(Alert.AlertType.INFORMATION, "Success", "Export successful!\nFile saved at: " + file.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
                showSimpleAlert(Alert.AlertType.ERROR, "Export Error", "Error saving file: " + e.getMessage());
            }
        }
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
                paymentService.updatePaymentStatus(p, "PAID");
            }
            payrollTable.refresh();
            updateSummaryCards(filteredData);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

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
                double employeeTaxRate = taxRate * (1 - empShare);

                paymentService.updateBonus(payment, newBonus, employeeTaxRate);
                loadData();
            } catch (NumberFormatException e) {
                showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid bonus amount!");
            }
        });
    }

    private void showPaymentDetails(Payment p) {
        String currency = settingService.getString(KEY_CURRENCY, "€");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Analysis");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());
        styleAlert(alert);

        double deductions = (p.getDeductions() != null) ? p.getDeductions() : 0.0;
        double employerCost = (p.getEmployerTax() != null) ? p.getEmployerTax() : 0.0;
        double totalStateTax = deductions + employerCost;
        double bonus = (p.getBonus() != null) ? p.getBonus() : 0.0;
        String ssn = (p.getEmployee().getSsn() != null) ? p.getEmployee().getSsn() : "-";

        String content = String.format(
                "SSN:              %s\n" +
                        "Month:            %s\n" +
                        "Status:           %s\n" +
                        "-----------------------------\n" +
                        "Base Salary:      %s%.2f\n" +
                        "Bonus:            %s%.2f\n" +
                        "GROSS PAY:        %s%.2f\n" +
                        "-----------------------------\n" +
                        "TAX & COSTS:\n" +
                        "  - Deductions:  -%s%.2f (Employee Pays)\n" +
                        "  - Employer Cost: %s%.2f (Company Pays)\n" +
                        "  (Total to State: %s%.2f)\n" +
                        "-----------------------------\n" +
                        "NET PAY:          %s%.2f",
                ssn, p.getMonthYear(), p.getStatus(),
                currency, p.getBaseSalary(),
                currency, bonus,
                currency, p.getGrossPay(),
                currency, deductions,
                currency, employerCost,
                currency, totalStateTax,
                currency, p.getAmount()
        );
        alert.setContentText(content);
        alert.showAndWait();
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
        DialogPane dialogPane = dialog.getDialogPane();
        URL cssResource = getClass().getResource("/theme.css");
        if (cssResource != null) {
            dialogPane.getStylesheets().add(cssResource.toExternalForm());
            dialogPane.getStyleClass().add("my-dialog");
        }
    }

    // --- TABLE SETUP & DATA LOADING ---

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colSsn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEmployee().getSsn() != null ? cell.getValue().getEmployee().getSsn() : "-"));

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
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
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
                        showSimpleAlert(Alert.AlertType.WARNING, "Locked", "Cannot edit a PAID payment!");
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
        long pendingCount = currentList.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getStatus())).count();
        lblPendingCount.setText(String.valueOf(pendingCount));

        if (currentList.isEmpty()) setButtonsVisible(true, false);
        else if (pendingCount > 0) setButtonsVisible(false, true);
        else setButtonsVisible(false, false);
    }

    private void setButtonsVisible(boolean generate, boolean finalizeBtn) {
        if (btnGenerate != null) {
            btnGenerate.setVisible(generate);
            btnGenerate.setManaged(generate);
        }
        if (btnFinalize != null) {
            btnFinalize.setVisible(finalizeBtn);
            btnFinalize.setManaged(finalizeBtn);
        }
    }
}