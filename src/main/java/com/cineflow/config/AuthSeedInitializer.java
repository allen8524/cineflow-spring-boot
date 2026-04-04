package com.cineflow.config;

import com.cineflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthSeedInitializer {

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:admin1234!}")
    private String adminPassword;

    @Value("${ADMIN_EMAIL:admin@cineflow.local}")
    private String adminEmail;

    @Value("${ADMIN_NAME:CineFlow 관리자}")
    private String adminName;

    @Value("${ADMIN_PHONE:010-0000-0000}")
    private String adminPhone;

    @Bean
    @ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
    CommandLineRunner initAuthSeed(UserService userService) {
        return args -> {
            boolean usingDefaultPassword = "admin1234!".equals(adminPassword);
            if (userService.findByLoginId(adminUsername).isEmpty()) {
                userService.createAdminIfAbsent(adminUsername, adminEmail, adminPassword, adminName, adminPhone);
                log.info("Seed ADMIN account is ready. loginId={}, password={}", adminUsername, usingDefaultPassword ? adminPassword : "[CUSTOM]");
            }
        };
    }
}
