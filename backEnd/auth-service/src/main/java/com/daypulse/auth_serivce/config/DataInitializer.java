package com.daypulse.auth_serivce.config;

import com.daypulse.auth_serivce.entity.UserAuth;
import com.daypulse.auth_serivce.enums.RoleEnum;
import com.daypulse.auth_serivce.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data Initializer
 * 
 * Seeds initial data into the database on application startup.
 * Creates default admin user if not exists.
 * 
 * Note: Only runs if no admin user exists to avoid duplicate admin creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataInitializer implements ApplicationRunner {
    
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("Running data initialization...");
        
        // Create default admin user if no admin exists
        createDefaultAdminIfNotExists();
        
        log.info("Data initialization completed");
    }
    
    private void createDefaultAdminIfNotExists() {
        // Check if any admin user exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == RoleEnum.ADMIN);
        
        if (!adminExists) {
            String adminEmail = "admin@daypulse.com";
            String adminPassword = "Admin@123"; // Change this in production!
            
            // Check if user with admin email already exists (but maybe with different role)
            if (userRepository.existsByEmail(adminEmail)) {
                log.warn("Admin email {} already exists but with non-admin role. Skipping admin creation.", adminEmail);
                return;
            }
            
            // Note: With Keycloak integration, admin user should be created in Keycloak
            // This local user record will be linked via keycloakId after Keycloak user creation
            UserAuth admin = UserAuth.builder()
                    .email(adminEmail)
                    .role(RoleEnum.ADMIN)
                    .isEmailVerified(true)
                    .isSetupComplete(true)
                    // keycloakId will be set after Keycloak user creation
                    .build();
            
            userRepository.save(admin);
            
            log.info("=====================================================");
            log.info("DEFAULT ADMIN USER CREATED");
            log.info("Email: {}", adminEmail);
            log.info("Password: {}", adminPassword);
            log.info("IMPORTANT: Change the admin password after first login!");
            log.info("=====================================================");
        } else {
            log.info("Admin user already exists, skipping creation");
        }
    }
}
