package com.naas.backend.auth.service;

import com.naas.backend.admin.AdminRepository;
import com.naas.backend.auth.dto.AuthResponse;
import com.naas.backend.auth.dto.LoginRequest;
import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService — Feature 1 (FR-AU2, FR-AU3, FR-AU6)
 *
 * SRS Reference:
 *   FR-AU2: The system shall verify user credentials before granting access.
 *   FR-AU3: The system shall restrict access based on user role.
 *   FR-AU6: The system shall display an error message for invalid login attempts.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private DeliveryPersonRepository deliveryPersonRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User customerUser;
    private User adminUser;
    private User deliveryPersonUser;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .password("encoded_password")
                .role(User.Role.CUSTOMER)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .password("encoded_password")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        deliveryPersonUser = User.builder()
                .id(UUID.randomUUID())
                .email("dp@test.com")
                .password("encoded_password")
                .role(User.Role.DELIVERY_PERSON)
                .active(true)
                .build();
    }

    // ── FR-AU3: Role mismatch should be rejected ──────────────────────────────

    @Test
    @DisplayName("FR-AU3: Customer attempting to access Admin portal should be rejected")
    void login_shouldReject_whenCustomerTriesToLoginAsAdmin() {
        // Arrange — a real CUSTOMER account tries to log in via the Admin portal
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@test.com");
        request.setPassword("password");
        request.setExpectedRole("ADMIN"); // Customer is requesting Admin access

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customerUser));

        // Act & Assert — the system must throw an error (FR-AU3 enforcement)
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

        // Verify: authenticationManager must NOT have been called (rejected early)
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("FR-AU3: Delivery Person attempting to access Customer portal should be rejected")
    void login_shouldReject_whenDeliveryPersonTriesToLoginAsCustomer() {
        // Arrange — a DELIVERY_PERSON account tries to log in via the Customer portal
        LoginRequest request = new LoginRequest();
        request.setEmail("dp@test.com");
        request.setPassword("password");
        request.setExpectedRole("CUSTOMER"); // DP trying to access Customer portal

        when(userRepository.findByEmail("dp@test.com"))
                .thenReturn(Optional.of(deliveryPersonUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

        verify(authenticationManager, never()).authenticate(any());
    }

    // ── FR-AU3: Correct role should pass role check and proceed ───────────────

    @Test
    @DisplayName("FR-AU3: Customer logging in to Customer portal should pass role check")
    void login_shouldPassRoleCheck_whenCustomerLogsIntoCustomerPortal() {
        // Arrange — a customer logs into the correct portal
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@test.com");
        request.setPassword("password");
        request.setExpectedRole("CUSTOMER");

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customerUser));
        when(jwtService.generateToken(customerUser)).thenReturn("mocked.jwt.token");
        when(customerRepository.findByUser(customerUser)).thenReturn(Optional.empty());
        // authenticationManager.authenticate() does nothing (success) by default with Mockito

        // Act
        AuthResponse response = authService.login(request);

        // Assert — a token was returned, the role is correct
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");

        // Verify: authentication was actually attempted
        verify(authenticationManager).authenticate(
                argThat(token -> token instanceof UsernamePasswordAuthenticationToken
                        && token.getPrincipal().equals("customer@test.com"))
        );
    }

    // ── FR-AU2: Login with no expectedRole (no portal guard) still authenticates ──

    @Test
    @DisplayName("FR-AU2: Login without expectedRole should not apply role guard")
    void login_shouldSucceed_whenNoExpectedRoleIsProvided() {
        // Arrange — request with no expectedRole (no cross-portal guard needed)
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password");
        request.setExpectedRole(null);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminUser));
        when(jwtService.generateToken(adminUser)).thenReturn("admin.jwt.token");
        when(adminRepository.findByUser(adminUser)).thenReturn(Optional.empty());

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response.getToken()).isEqualTo("admin.jwt.token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    // ── FR-AU6: Non-existent user produces error ──────────────────────────────

    @Test
    @DisplayName("FR-AU6: Login with wrong role constraint propagates error message correctly")
    void login_shouldThrowRuntimeException_withCorrectMessage_onRoleMismatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@test.com");
        request.setPassword("wrongpassword");
        request.setExpectedRole("ADMIN");

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");
    }
}
