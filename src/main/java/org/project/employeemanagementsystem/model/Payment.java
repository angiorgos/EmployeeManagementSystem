package org.project.employeemanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate paymentDate;
    private String monthYear;

    private Double baseSalary;
    private Double hoursWorked;
    private Double overtimeHours;
    private Double sundayHours;

    private Double grossPay;
    private Double deductions; // Employee Share (15%)

    private Double employerTax;
    private Double totalTax;   // Total State Tax (Employer 30% + Employee 15% = 45%)

    private Double amount;     // Net Pay
    private Double bonus;
    private String status;
}