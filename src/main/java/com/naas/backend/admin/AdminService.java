package com.naas.backend.admin;

import com.naas.backend.admin.dto.CreateAdminRequest;
import com.naas.backend.admin.dto.CreateDeliveryPersonRequest;
import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, String> createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(user);

        Admin admin = Admin.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .employeeId(request.getEmployeeId())
                .build();
        adminRepository.save(admin);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin created successfully");
        response.put("email", request.getEmail());
        response.put("name", request.getName());
        return response;
    }

    @Transactional
    public Map<String, String> createDeliveryPerson(CreateDeliveryPersonRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.DELIVERY_PERSON)
                .active(true)
                .build();
        userRepository.save(user);

        DeliveryPerson dp = DeliveryPerson.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .employeeId(request.getEmployeeId())
                .payoutDetails(request.getPayoutDetails())
                .build();
        deliveryPersonRepository.save(dp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Delivery person created successfully");
        response.put("email", request.getEmail());
        response.put("name", request.getName());
        return response;
    }

    public Map<String, String> getMyProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Admin admin = adminRepository.findByUser(user).orElseGet(() -> {
            Admin defaultAdmin = Admin.builder()
                    .user(user)
                    .name("System Admin")
                    .build();
            return adminRepository.save(defaultAdmin);
        });
        
        Map<String, String> response = new HashMap<>();
        if (admin.getName() != null) response.put("name", admin.getName());
        if (admin.getPhone() != null) response.put("phone", admin.getPhone());
        if (admin.getEmployeeId() != null) response.put("employeeId", admin.getEmployeeId());
        response.put("email", email);
        return response;
    }

    @Transactional
    public Map<String, String> updateMyProfile(String email, Map<String, String> updates) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Admin admin = adminRepository.findByUser(user).orElseGet(() -> {
            Admin defaultAdmin = Admin.builder()
                    .user(user)
                    .name("System Admin")
                    .build();
            return adminRepository.save(defaultAdmin);
        });
        
        if (updates.containsKey("name")) admin.setName(updates.get("name"));
        if (updates.containsKey("phone")) admin.setPhone(updates.get("phone"));
        if (updates.containsKey("employeeId")) admin.setEmployeeId(updates.get("employeeId"));
        
        adminRepository.save(admin);
        return getMyProfile(email);
    }

    public java.util.List<com.naas.backend.admin.dto.AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream().map(this::mapToResponse).collect(java.util.stream.Collectors.toList());
    }

    public com.naas.backend.admin.dto.AdminResponse getAdminById(java.util.UUID id) {
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        return mapToResponse(admin);
    }

    @Transactional
    public com.naas.backend.admin.dto.AdminResponse updateAdmin(java.util.UUID id, com.naas.backend.admin.dto.UpdateAdminRequest request) {
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        if (request.getName() != null) admin.setName(request.getName());
        if (request.getPhone() != null) admin.setPhone(request.getPhone());
        if (request.getEmployeeId() != null) admin.setEmployeeId(request.getEmployeeId());
        return mapToResponse(adminRepository.save(admin));
    }

    @Transactional
    public com.naas.backend.admin.dto.AdminResponse toggleStatus(java.util.UUID id, boolean active) {
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        admin.getUser().setActive(active);
        userRepository.save(admin.getUser());
        return mapToResponse(admin);
    }

    @Transactional
    public void deleteAdmin(java.util.UUID id) {
        Admin admin = adminRepository.findById(id).orElseThrow(() -> new RuntimeException("Admin not found"));
        admin.getUser().setActive(false); // soft delete
        userRepository.save(admin.getUser());
    }

    private com.naas.backend.admin.dto.AdminResponse mapToResponse(Admin admin) {
        return com.naas.backend.admin.dto.AdminResponse.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getUser().getEmail())
                .phone(admin.getPhone())
                .employeeId(admin.getEmployeeId())
                .active(admin.getUser().isActive())
                .build();
    }
}
