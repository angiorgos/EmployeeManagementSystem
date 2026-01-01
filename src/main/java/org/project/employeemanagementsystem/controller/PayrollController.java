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

    private final PaymentService paymentService;
    private final EmployeeService employeeService;
    private final SystemSettingService settingService;

    // --- KEYS SETTINGS ---
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";
    private static final String KEY_TAX = "payroll.total_tax_rate";
    private static final String KEY_EMPLOYER_SHARE = "payroll.employer_share";
    // Άλλα κλειδιά για Night/Holiday/Company αν θέλεις...
    private static final String KEY_COMPANY_NAME = "company.name";
    private static final String KEY_CURRENCY = "company.currency";

    public PayrollController(PaymentService paymentService, EmployeeService employeeService, SystemSettingService settingService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
        this.settingService = settingService;
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
        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0); // Πρότυπες Ώρες
        double otRate   = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate  = settingService.getDouble(KEY_SUNDAY, 1.75);
        double taxRate  = settingService.getDouble(KEY_TAX, 0.40);
        double empSplit = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;

            // 2. Κλήση Service με τις σωστές παραμέτρους
            paymentService.calculateAndSavePayroll(
                    emp,
                    selectedDate,
                    stdHours, // <-- Περνάμε το 176 εδώ
                    0.0, 0.0, // Overtime & Sunday (αρχικά 0)
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
        TextField taxField = new TextField(String.valueOf(settingService.getDouble(KEY_TAX, 0.40)));
        TextField splitField = new TextField(String.valueOf(settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60)));
        TextField otField = new TextField(String.valueOf(settingService.getDouble(KEY_OVERTIME, 1.50)));
        TextField currencyField = new TextField(settingService.getString(KEY_CURRENCY, "€"));

        grid.addRow(0, new Label("Standard Monthly Hours:"), hoursField);
        grid.addRow(1, new Label("Overtime Rate:"), otField);
        grid.addRow(2, new Label("Total Tax Rate (0.xx):"), taxField);
        grid.addRow(3, new Label("Employer Share (0.xx):"), splitField);
        grid.addRow(4, new Label("Currency:"), currencyField);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtnType) {
                try {
                    settingService.save(KEY_WORK_HOURS, Double.parseDouble(hoursField.getText()));
                    settingService.save(KEY_OVERTIME, Double.parseDouble(otField.getText()));
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

                // Χρειαζόμαστε τα rates για να υπολογίσουμε σωστά τους φόρους στο Service
                double taxRate = settingService.getDouble(KEY_TAX, 0.40);
                double empShare = settingService.getDouble(KEY_EMPLOYER_SHARE, 0.60);

                // Κλήση της μεθόδου Update
                paymentService.updateBonus(payment, newBonus, taxRate, empShare);

                loadData();
            } catch (NumberFormatException e) {
                showSimpleAlert(Alert.AlertType.ERROR, "Error", "Invalid bonus amount!");
            }
        });
    }

    @FXML
    public void exportToExcel() {
        // ... (Ο κώδικας του Export μένει ίδιος όπως πριν) ...
        // Αν θες να στον ξαναγράψω, πες μου, αλλά είναι μεγάλος και δεν άλλαξε.
        showSimpleAlert(Alert.AlertType.INFORMATION, "Export", "Excel export logic goes here.");
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

    // --- UI SETUP ---

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
            private final Button btnBonus = new Button("Bonus");
            private final HBox pane = new HBox(5, btnBonus);
            {
                btnBonus.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size:11px;");
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