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
}
