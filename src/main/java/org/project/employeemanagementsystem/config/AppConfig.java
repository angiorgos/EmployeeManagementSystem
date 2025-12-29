package org.project.employeemanagementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean // Αυτό φτιάχνει τον "PasswordEncoder" που ψάχνει το AuthService
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}