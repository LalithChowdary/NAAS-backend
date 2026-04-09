package com.naas.backend.customer;

import java.util.UUID;

import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.customer.dto.CreateCustomerByAdminRequest;
import com.naas.backend.customer.dto.CustomerResponse;
import com.naas.backend.customer.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ----------------------------------------------------------------
    // Helper: map Customer entity → CustomerResponse DTO
    // ----------------------------------------------------------------
    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getUser().getEmail())
                .phone(customer.getPhone())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    // ----------------------------------------------------------------
    // Customer self-service
    // ----------------------------------------------------------------

    public CustomerResponse getProfile(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateProfile(User user, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        if (request.getName() != null)
            customer.setName(request.getName());
        if (request.getPhone() != null)
            customer.setPhone(request.getPhone());

        customerRepository.save(customer);
        return toResponse(customer);
    }

    @Transactional
    public void deactivateAccount(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        customer.setActive(false);
        customerRepository.save(customer);

        user.setActive(false);
        userRepository.save(user);
    }

    // ----------------------------------------------------------------
    // Admin operations
    // ----------------------------------------------------------------

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> searchCustomers(String query) {
        return customerRepository
                .findByNameContainingIgnoreCaseOrPhoneContaining(query, query)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CustomerResponse getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerByAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)
                .active(true)
                .build();
        userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .active(true)
                .build();
        customerRepository.save(customer);

        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        if (request.getName() != null)
            customer.setName(request.getName());
        if (request.getPhone() != null)
            customer.setPhone(request.getPhone());

        customerRepository.save(customer);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse toggleStatus(UUID id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setActive(active);
        customerRepository.save(customer);

        // Also toggle the User account so login is blocked when inactive
        customer.getUser().setActive(active);
        userRepository.save(customer.getUser());

        return toResponse(customer);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        // Soft delete — deactivate both Customer and User
        customer.setActive(false);
        customerRepository.save(customer);

        customer.getUser().setActive(false);
        userRepository.save(customer.getUser());
    }
}
