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

    // --- DATABASE CONFIG KEYS ---
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";
    private static final String KEY_TAX = "payroll.total_tax_rate";
    private static final String KEY_EMPLOYER_SHARE = "payroll.employer_share";

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
        loadData();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        monthPicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ============================================================
    // MAIN ACTIONS
    // ============================================================

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a month first!");
            return;
        }

        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double otRate = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate = settingService.getDouble(KEY_SUNDAY, 1.75);
        double taxRate = settingService.getDouble(KEY_TAX, 0.40);
        double empSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;

            paymentService.calculateAndSavePayroll(
                    emp, selectedDate, stdHours,
                    0.0, 0.0,
                    otRate, sunRate, taxRate, empSplit
            );
            count++;
        }

        loadData();
        String monthStr = selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        showSimpleAlert(Alert.AlertType.INFORMATION, "Success",
                "Generated payroll for " + count + " employees (" + monthStr + ")\nUsing Standard Hours: " + stdHours);
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

                String[] columns = {
                        "ID", "SSN", "Name", "Month",
                        "Base Salary", "Bonus", "Gross Pay",
                        "Deductions (Employee)", "Employer Cost",
                        "Net Pay", "Status"
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

                    org.apache.poi.ss.usermodel.Cell ssnCell = row.createCell(1);
                    if (p.getEmployee().getSsn() != null) {
                        ssnCell.setCellValue(p.getEmployee().getSsn());
                    } else {
                        ssnCell.setCellValue("-");
                    }

                    row.createCell(2).setCellValue(p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName());
                    row.createCell(3).setCellValue(p.getMonthYear());

                    org.apache.poi.ss.usermodel.Cell baseCell = row.createCell(4);
                    if (p.getBaseSalary() != null) {
                        baseCell.setCellValue(p.getBaseSalary());
                    } else {
                        baseCell.setCellValue(0.0);
                    }

                    org.apache.poi.ss.usermodel.Cell bonusCell = row.createCell(5);
                    if (p.getBonus() != null) {
                        bonusCell.setCellValue(p.getBonus());
                    } else {
                        bonusCell.setCellValue(0.0);
                    }

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
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configuration");
        dialog.setHeaderText("Payroll & Tax Settings");
        styleAlert(dialog);

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        double curHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double curOt = settingService.getDouble(KEY_OVERTIME, 1.50);
        double curSun = settingService.getDouble(KEY_SUNDAY, 1.75);
        double curTax = settingService.getDouble(KEY_TAX, 0.40);
        double curSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        TextField hoursField = new TextField(String.valueOf(curHours));
        TextField otField = new TextField(String.valueOf(curOt));
        TextField sunField = new TextField(String.valueOf(curSun));
        TextField taxField = new TextField(String.valueOf(curTax));
        TextField splitField = new TextField(String.valueOf(curSplit));

        String fieldStyle = "-fx-background-radius: 4; -fx-border-color: #D1D5DB; -fx-border-radius: 4;";
        hoursField.setStyle(fieldStyle); otField.setStyle(fieldStyle); sunField.setStyle(fieldStyle);
        taxField.setStyle(fieldStyle); splitField.setStyle(fieldStyle);

        grid.addRow(0, new Label("Std Monthly Hours:"), hoursField);
        grid.addRow(1, new Label("Overtime Rate (x):"), otField);
        grid.addRow(2, new Label("Sunday Rate (x):"), sunField);
        grid.addRow(3, new Label("Total Tax Rate (0.xx):"), taxField);
        grid.addRow(4, new Label("Employer Split (0.xx):"), splitField);
        grid.add(new Label("(e.g. 0.60 means Employer pays 60% of tax)"), 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtnType) {
                try {
                    settingService.setDouble(KEY_WORK_HOURS, Double.parseDouble(hoursField.getText()));
                    settingService.setDouble(KEY_OVERTIME, Double.parseDouble(otField.getText()));
                    settingService.setDouble(KEY_SUNDAY, Double.parseDouble(sunField.getText()));
                    settingService.setDouble(KEY_TAX, Double.parseDouble(taxField.getText()));
                    settingService.setDouble(KEY_EMPLOYER_SHARE, Double.parseDouble(splitField.getText()));

                    showSimpleAlert(Alert.AlertType.INFORMATION, "Saved", "Settings updated successfully!");
                } catch (Exception e) {
                    showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid numbers provided!");
                }
            }
        });
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void openBonusDialog(Payment payment) {
        String currentBonus;
        if (payment.getBonus() != null) {
            currentBonus = payment.getBonus().toString();
        } else {
            currentBonus = "0.0";
        }

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Analysis");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());
        styleAlert(alert);

        double deductions;
        if (p.getDeductions() != null) {
            deductions = p.getDeductions();
        } else {
            deductions = 0.0;
        }

        double employerCost;
        if (p.getEmployerTax() != null) {
            employerCost = p.getEmployerTax();
        } else {
            employerCost = 0.0;
        }

        double totalStateTax = deductions + employerCost;

        double bonus;
        if (p.getBonus() != null) {
            bonus = p.getBonus();
        } else {
            bonus = 0.0;
        }

        String ssn;
        if (p.getEmployee().getSsn() != null) {
            ssn = p.getEmployee().getSsn();
        } else {
            ssn = "-";
        }

        String content = String.format(
                "SSN:              %s\n" +
                        "Month:            %s\n" +
                        "Status:           %s\n" +
                        "-----------------------------\n" +
                        "Base Salary:      €%.2f\n" +
                        "Bonus:            €%.2f\n" +
                        "GROSS PAY:        €%.2f\n" +
                        "-----------------------------\n" +
                        "TAX & COSTS:\n" +
                        "  - Deductions:  -€%.2f (Employee Pays)\n" +
                        "  - Employer Cost: €%.2f (Company Pays)\n" +
                        "  (Total to State: €%.2f)\n" +
                        "-----------------------------\n" +
                        "NET PAY:          €%.2f",
                ssn, p.getMonthYear(), p.getStatus(), p.getBaseSalary(), bonus,
                p.getGrossPay(), deductions, employerCost, totalStateTax, p.getAmount()
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

        colSsn.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            String ssnValue;
            if (emp != null && emp.getSsn() != null) {
                ssnValue = emp.getSsn();
            } else {
                ssnValue = "-";
            }
            return new SimpleStringProperty(ssnValue);
        });

        colName.setCellValueFactory(cell -> {
            Employee emp = cell.getValue().getEmployee();
            String fullName;
            if (emp != null) {
                fullName = emp.getLastName() + " " + emp.getFirstName();
            } else {
                fullName = "Unknown";
            }
            return new SimpleStringProperty(fullName);
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
                    if ("PAID".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else if ("PENDING".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #EF4444;");
                    }
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

        if (currentList.isEmpty()) {
            setButtonsVisible(true, false);
        } else if (pendingCount > 0) {
            setButtonsVisible(false, true);
        } else {
            setButtonsVisible(false, false);
        }
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