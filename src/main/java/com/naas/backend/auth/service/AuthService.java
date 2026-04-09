package com.naas.backend.auth.service;

import com.naas.backend.admin.Admin;
import com.naas.backend.admin.AdminRepository;
import com.naas.backend.auth.dto.AuthResponse;
import com.naas.backend.auth.dto.LoginRequest;
import com.naas.backend.auth.dto.SignupRequest;
import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final CustomerRepository customerRepository;
        private final AdminRepository adminRepository;
        private final DeliveryPersonRepository deliveryPersonRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthResponse login(LoginRequest request) {
                User checkUser = userRepository.findByEmail(request.getEmail()).orElse(null);
                if (checkUser != null && request.getExpectedRole() != null) {
                        if (!checkUser.getRole().name().equalsIgnoreCase(request.getExpectedRole())) {
                                throw new RuntimeException("Invalid email or password");
                        }
                }

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
                } catch (org.springframework.security.authentication.AccountStatusException e) {
                        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("Account disabled"));
                        if (user.getRole() == User.Role.DELIVERY_PERSON) {
                                DeliveryPerson dp = deliveryPersonRepository.findByUser(user).orElse(null);
                                if (dp != null) {
                                        if ("PENDING".equals(dp.getStatus())) {
                                                throw new RuntimeException("Your application is still pending review.");
                                        } else if ("REJECTED".equals(dp.getStatus())) {
                                                throw new RuntimeException("Your application has been rejected.");
                                        }
                                }
                        }
                        throw new RuntimeException("Account disabled. Please contact support.");
                } catch (org.springframework.security.authentication.BadCredentialsException e) {
                        throw new RuntimeException("Invalid email or password");
                } catch (Exception e) {
                        throw new RuntimeException("Authentication failed");
                }

                User user = checkUser != null ? checkUser : userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String token = jwtService.generateToken(user);
                String displayName = getDisplayName(user);

                return AuthResponse.builder()
                                .token(token)
                                .role(user.getRole().name())
                                .name(displayName)
                                .build();
        }

        @Transactional
        public AuthResponse registerCustomer(SignupRequest request) {
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
                                .build();
                customerRepository.save(customer);

                String token = jwtService.generateToken(user);

                return AuthResponse.builder()
                                .token(token)
                                .role(user.getRole().name())
                                .name(request.getName())
                                .build();
        }

        private String getDisplayName(User user) {
                switch (user.getRole()) {
                        case CUSTOMER:
                                return customerRepository.findByUser(user)
                                                .map(Customer::getName)
                                                .orElse(user.getEmail());
                        case ADMIN:
                                return adminRepository.findByUser(user)
                                                .map(Admin::getName)
                                                .orElse(user.getEmail());
                        case DELIVERY_PERSON:
                                return deliveryPersonRepository.findByUser(user)
                                                .map(DeliveryPerson::getName)
                                                .orElse(user.getEmail());
                        default:
                                return user.getEmail();
                }
        }
}
