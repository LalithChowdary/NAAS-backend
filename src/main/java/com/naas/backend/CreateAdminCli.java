package com.naas.backend;

import com.naas.backend.admin.Admin;
import com.naas.backend.admin.AdminRepository;
import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Scanner;

public class CreateAdminCli {

    public static void main(String[] args) {
        System.out.println("Starting NAAS Admin Creation CLI...");
        System.out.println("Booting repository context (this may take a few seconds)...");

        // Start Spring application without starting the web server
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        UserRepository userRepository = context.getBean(UserRepository.class);
        AdminRepository adminRepository = context.getBean(AdminRepository.class);
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);

        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter Admin Email: ");
        String email = scanner.nextLine().trim();

        // Check if user exists using the proper finder logic available in the repo if existsByEmail isn't there, we can find by email
        if (userRepository.findByEmail(email).isPresent()) {
            System.err.println("User with this email already exists!");
            context.close();
            System.exit(1);
        }

        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine().trim();
        
        System.out.print("Enter Admin Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Employee ID: ");
        String employeeId = scanner.nextLine().trim();

        System.out.print("Enter Phone: ");
        String phone = scanner.nextLine().trim();

        // Build User entity
        User adminUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.ADMIN)
                .active(true)
                .build();
        
        userRepository.save(adminUser);

        // Build Admin profile entity
        Admin adminProfile = Admin.builder()
                .user(adminUser)
                .name(name)
                .employeeId(employeeId)
                .phone(phone)
                .build();
                
        adminRepository.save(adminProfile);

        System.out.println("\n✅ Admin successfully created in the database!");
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        
        context.close();
        System.exit(0); // Exit process immediately to kill any lingering threads
    }
}
