package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.employeemanagementsystem.model.Payment;
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
    private final SystemSettingService settingService;

    // --- KEYS (Συμβατά με τη νέα δομή της βάσης) ---
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";

    // Ρεαλιστικά Keys
    private static final String KEY_SOCIAL_RATE = "payroll.social_rate";       // Social Security (EFKA)
    private static final String KEY_INCOME_TAX_RATE = "payroll.income_tax_rate"; // Income Tax (FMY)
    private static final String KEY_EMPLOYER_RATE = "payroll.employer_rate";   // Employer Cost

    private static final String KEY_CURRENCY = "company.currency";

    public PayrollController(PaymentService paymentService, SystemSettingService settingService) {
        this.paymentService = paymentService;
        this.settingService = settingService;
    }

    // --- FXML FIELDS ---
    @FXML private Label lblPendingCount;
    @FXML private DatePicker monthPicker;
    @FXML private TextField searchField;
    @FXML private Button btnGenerate;
    @FXML private Button btnFinalize;
    @FXML private VBox loadingOverlay;

    @FXML private TableView<Payment> payrollTable;
    @FXML private TableColumn<Payment, Long> colId;
    @FXML private TableColumn<Payment, String> colSsn;
    @FXML private TableColumn<Payment, String> colName;
    @FXML private TableColumn<Payment, String> colMonth;
    @FXML private TableColumn<Payment, Double> colGross;

    // Οι στήλες κρατήσεων
    @FXML private TableColumn<Payment, Double> colDeductions; // Social Security
    @FXML private TableColumn<Payment, Double> colTax;        // Income Tax
    @FXML private TableColumn<Payment, Double> colEmployerTax;

    @FXML private TableColumn<Payment, Double> colAmount;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, Void> colActions;

    private final ObservableList<Payment> masterData = FXCollections.observableArrayList();
    private FilteredList<Payment> filteredData;
    private SortedList<Payment> sortedData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        monthPicker.setValue(LocalDate.now());
        setupTableColumns();

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(payrollTable.comparatorProperty());
        payrollTable.setItems(sortedData);
        payrollTable.getSortOrder().add(colId);

        Platform.runLater(this::loadData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        monthPicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadData() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<List<Payment>> task = new Task<>() {
            @Override
            protected List<Payment> call() {
                return paymentService.getAllPayments();
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            applyFilters();
            fadeOutLoading();
        });

        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            showSimpleAlert(Alert.AlertType.ERROR, "Error", "Failed to load: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    //ACTIONS

    @FXML
    public void generatePayroll() {
        LocalDate selectedDate = monthPicker.getValue();
        if (selectedDate == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a month first!");
            return;
        }

        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                paymentService.generateMonthlyPayroll(selectedDate);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            loadData();
            showSimpleAlert(Alert.AlertType.INFORMATION, "Success", "Payroll generated successfully!");
        });

        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            showSimpleAlert(Alert.AlertType.ERROR, "Calculation Failed", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    public void finalizePayments() {
        List<Payment> pending = filteredData.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            showSimpleAlert(Alert.AlertType.WARNING, "No Pending Payments", "All listed payments are already paid!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Mark " + pending.size() + " payments as PAID?");
        styleAlert(alert);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        loadingOverlay.setVisible(true);
        loadingOverlay.setOpacity(1.0);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                for (Payment p : pending) {
                    paymentService.finalizePayment(p);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            loadData();
            showSimpleAlert(Alert.AlertType.INFORMATION, "Success", "Payments finalized!");
        });

        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            showSimpleAlert(Alert.AlertType.ERROR, "Error", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    //SETTINGS & EXCEL

    @FXML
    public void openSettingsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Payroll Configuration");
        styleAlert(dialog);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField hoursField = new TextField(String.valueOf(settingService.getDouble(KEY_WORK_HOURS, 176.0)));
        TextField otField = new TextField(String.valueOf(settingService.getDouble(KEY_OVERTIME, 1.50)));
        TextField sunField = new TextField(String.valueOf(settingService.getDouble(KEY_SUNDAY, 1.75)));

        TextField socialField = new TextField(String.valueOf(settingService.getDouble(KEY_SOCIAL_RATE, 0.14)));
        TextField taxField = new TextField(String.valueOf(settingService.getDouble(KEY_INCOME_TAX_RATE, 0.10)));
        TextField employerField = new TextField(String.valueOf(settingService.getDouble(KEY_EMPLOYER_RATE, 0.22)));

        TextField currencyField = new TextField(settingService.getString(KEY_CURRENCY, "€"));

        grid.addRow(0, new Label("Standard Monthly Hours:"), hoursField);
        grid.addRow(1, new Label("Overtime Rate (x):"), otField);
        grid.addRow(2, new Label("Sunday Rate (x):"), sunField);
        grid.addRow(3, new Label("Social Security Rate (0.xx):"), socialField);
        grid.addRow(4, new Label("Income Tax Rate (0.xx):"), taxField);
        grid.addRow(5, new Label("Employer Cost Rate (0.xx):"), employerField);
        grid.addRow(6, new Label("Currency:"), currencyField);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtn) {
                try {
                    settingService.save(KEY_WORK_HOURS, Double.parseDouble(hoursField.getText()));
                    settingService.save(KEY_OVERTIME, Double.parseDouble(otField.getText()));
                    settingService.save(KEY_SUNDAY, Double.parseDouble(sunField.getText()));

                    settingService.save(KEY_SOCIAL_RATE, Double.parseDouble(socialField.getText()));
                    settingService.save(KEY_INCOME_TAX_RATE, Double.parseDouble(taxField.getText()));
                    settingService.save(KEY_EMPLOYER_RATE, Double.parseDouble(employerField.getText()));

                    settingService.save(KEY_CURRENCY, currencyField.getText());
                    showSimpleAlert(Alert.AlertType.INFORMATION, "Saved", "Settings updated!");
                } catch (Exception e) {
                    showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid input! Please use numbers (e.g., 0.14).");
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
        fileChooser.setInitialFileName("Payroll_Export_" + LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(payrollTable.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Payroll Data");
                String currency = settingService.getString(KEY_CURRENCY, "€");

                // Διεθνείς τίτλοι στο Excel
                String[] columns = {
                        "ID", "SSN", "Name", "Month",
                        "Base Salary", "Overtime Hrs", "Sunday Hrs", "Bonus",
                        "Gross Pay", "Social Security", "Income Tax",
                        "Employer Cost", "Net Pay (" + currency + ")", "Status"
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
                    row.createCell(9).setCellValue(p.getDeductions());      // Social Security
                    row.createCell(10).setCellValue(p.getTotalTax() != null ? p.getTotalTax() : 0.0); // Income Tax
                    row.createCell(11).setCellValue(p.getEmployerTax());    // Employer Cost
                    row.createCell(12).setCellValue(p.getAmount());         // Net Pay
                    row.createCell(13).setCellValue(p.getStatus());
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

    //HELPER & UI

    private void showPaymentDetails(Payment p) {
        String currency = settingService.getString(KEY_CURRENCY, "€");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Breakdown");
        alert.setHeaderText("Payroll: " + p.getEmployee().getLastName());
        styleAlert(alert);

        double gross = p.getGrossPay();
        double social = (p.getDeductions() != null) ? p.getDeductions() : 0.0;
        double tax = (p.getTotalTax() != null) ? p.getTotalTax() : 0.0;
        double employerCost = (p.getEmployerTax() != null) ? p.getEmployerTax() : 0.0;
        double net = p.getAmount();

        // Ενημερωμένο κείμενο Info
        String content = String.format(
                "Month: %s | Status: %s\n" +
                        "-------------------------------------\n" +
                        "GROSS PAY:            %s%.2f\n" +
                        "-------------------------------------\n" +
                        "(-) Social Security:  %s%.2f\n" +
                        "(-) Income Tax:       %s%.2f\n" +
                        "-------------------------------------\n" +
                        "(=) NET PAY:          %s%.2f\n" +
                        "-------------------------------------\n" +
                        "(Employer Cost: %s%.2f)",
                p.getMonthYear(), p.getStatus(),
                currency, gross,
                currency, social,
                currency, tax,
                currency, net,
                currency, employerCost
        );
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openBonusDialog(Payment payment) {
        TextInputDialog dialog = new TextInputDialog(payment.getBonus() != null ? payment.getBonus().toString() : "0.0");
        dialog.setTitle("Add Bonus");
        dialog.setHeaderText("Bonus for: " + payment.getEmployee().getLastName());
        styleAlert(dialog);

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double newBonus = Double.parseDouble(amountStr);
                Payment updated = paymentService.updateBonus(payment, newBonus);

                int idx = masterData.indexOf(payment);
                if (idx >= 0) masterData.set(idx, updated);

                payrollTable.refresh();
                updateSummaryCards(filteredData);
            } catch (Exception e) {
                showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid Amount!");
            }
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSsn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEmployee().getSsn() != null ? cell.getValue().getEmployee().getSsn() : "-"));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getEmployee().getLastName() + " " + cell.getValue().getEmployee().getFirstName()));
        colMonth.setCellValueFactory(new PropertyValueFactory<>("monthYear"));
        colGross.setCellValueFactory(new PropertyValueFactory<>("grossPay"));

        // Σύνδεση με τη βάση (Social Security)
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));

        // Σύνδεση με τη βάση (Income Tax)
        colTax.setCellValueFactory(new PropertyValueFactory<>("totalTax"));

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
                btnInfo.getStyleClass().add("table-btn");
                btnInfo.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");
                btnInfo.setOnAction(e -> showPaymentDetails(getTableView().getItems().get(getIndex())));

                btnBonus.getStyleClass().add("table-btn");
                btnBonus.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white;");
                btnBonus.setOnAction(e -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    if ("PAID".equalsIgnoreCase(p.getStatus())) {
                        showSimpleAlert(Alert.AlertType.WARNING, "Locked", "Cannot edit bonus for PAID payments!");
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

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedMonth = (monthPicker.getValue() != null) ?
                monthPicker.getValue().format(DateTimeFormatter.ofPattern("MM/yyyy")) : null;

        filteredData.setPredicate(p -> {
            boolean matchesName = query.isEmpty() ||
                    (p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName()).toLowerCase().contains(query);
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

    private void setButtonsVisible(boolean listEmpty, boolean hasPending) {
        if (btnGenerate != null) { btnGenerate.setVisible(listEmpty); btnGenerate.setManaged(listEmpty); }
        if (btnFinalize != null) { btnFinalize.setVisible(hasPending); btnFinalize.setManaged(hasPending); }
    }

    private void fadeOutLoading() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(ev -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
            fadeOut.play();
        });
        delay.play();
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