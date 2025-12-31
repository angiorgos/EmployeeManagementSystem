package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.SystemSetting;
import org.project.employeemanagementsystem.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public double getDouble(String key, double defaultValue) {
        return repository.findById(key)
                .map(s -> Double.parseDouble(s.getSettingValue()))
                .orElse(defaultValue);
    }

    public void setDouble(String key, double value) {
        SystemSetting setting = new SystemSetting(key, String.valueOf(value));
        repository.save(setting);
    }
}