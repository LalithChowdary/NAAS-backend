package com.naas.backend.admin;

import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only seed when --seed-admin flag is passed
        if (!Arrays.asList(args).contains("--seed-admin")) {
            return;
        }

        if (userRepository.existsByRole(User.Role.ADMIN)) {
            log.info("Admin already exists — skipping seed.");
            return;
        }

        User adminUser = User.builder()
                .email("admin@naas.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(adminUser);

        Admin adminProfile = Admin.builder()
                .user(adminUser)
                .name("System Admin")
                .phone("0000000000")
                .employeeId("EMP001")
                .build();
        adminRepository.save(adminProfile);

        log.info("============================================");
        log.info("  DEFAULT ADMIN CREATED");
        log.info("  Email:    admin@naas.com");
        log.info("  Password: admin123");
        log.info("============================================");
    }
}
