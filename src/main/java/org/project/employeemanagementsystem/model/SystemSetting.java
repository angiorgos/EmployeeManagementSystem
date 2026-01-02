package org.project.employeemanagementsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.employeemanagementsystem.util.AuditListener;

@Entity
@Table(name = "system_settings")
@EntityListeners(AuditListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    private String settingKey;   // Το όνομα της ρύθμισης (π.χ. "payroll.tax_rate")

    private String settingValue; // Η τιμή της (π.χ. "0.40")
}