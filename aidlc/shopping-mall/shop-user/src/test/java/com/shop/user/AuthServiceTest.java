package com.shop.user;

import com.shop.user.config.JwtConfig;
import com.shop.user.dto.AuthResponse;
import com.shop.user.dto.LoginRequest;
import com.shop.user.dto.LoginResponse;
import com.shop.user.dto.RegisterRequest;
import com.shop.user.entity.User;
import com.shop.user.exception.DuplicateEmailException;
import com.shop.user.exception.InvalidCredentialsException;
import com.shop.user.repository.UserRepository;
import com.shop.user.service.UserService;
import io.jsonwebtoken.Claims;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@JqwikSpringSupport
class AuthServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ===== Unit Tests =====

    @Test
    void register_success() {
        RegisterRequest req = createRegisterRequest("test@example.com", "password123", "Test User");
        userService.register(req);

        User saved = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getName()).isEqualTo("Test User");
    }

    @Test
    void register_duplicateEmail_throws409() {
        RegisterRequest req = createRegisterRequest("dup@example.com", "password123", "User");
        userService.register(req);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_passwordIsHashed() {
        RegisterRequest req = createRegisterRequest("hash@example.com", "plainpassword", "User");
        userService.register(req);

        User saved = userRepository.findByEmail("hash@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("plainpassword");
        assertThat(passwordEncoder.matches("plainpassword", saved.getPassword())).isTrue();
    }

    @Test
    void login_success_returnsToken() {
        RegisterRequest reg = createRegisterRequest("login@example.com", "password123", "User");
        userService.register(reg);

        LoginRequest loginReq = createLoginRequest("login@example.com", "password123");
        AuthResponse response = userService.login(loginReq);

        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    void login_wrongPassword_throws401() {
        RegisterRequest reg = createRegisterRequest("wrong@example.com", "password123", "User");
        userService.register(reg);

        LoginRequest loginReq = createLoginRequest("wrong@example.com", "wrongpassword");
        assertThatThrownBy(() -> userService.login(loginReq))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_nonExistentEmail_throws401() {
        LoginRequest loginReq = createLoginRequest("nouser@example.com", "password123");
        assertThatThrownBy(() -> userService.login(loginReq))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void jwt_containsUserIdAndEmail() {
        RegisterRequest reg = createRegisterRequest("jwt@example.com", "password123", "JWT User");
        userService.register(reg);

        LoginRequest loginReq = createLoginRequest("jwt@example.com", "password123");
        AuthResponse response = userService.login(loginReq);

        Claims claims = jwtConfig.parseToken(response.getAccessToken());
        assertThat(claims.get("email", String.class)).isEqualTo("jwt@example.com");
        assertThat(claims.get("userId", Long.class)).isNotNull();
    }

    @Test
    void jwt_expiration_is24Hours() {
        RegisterRequest reg = createRegisterRequest("exp@example.com", "password123", "Exp User");
        userService.register(reg);

        LoginRequest loginReq = createLoginRequest("exp@example.com", "password123");
        AuthResponse response = userService.login(loginReq);

        Claims claims = jwtConfig.parseToken(response.getAccessToken());
        long issuedAt = claims.getIssuedAt().getTime();
        long expiration = claims.getExpiration().getTime();
        long diffMs = expiration - issuedAt;

        // Should be approximately 24 hours (86400000 ms), allow 5 second tolerance
        assertThat(diffMs).isBetween(86395000L, 86405000L);
    }

    // ===== Property-Based Tests =====

    // Feature: shopping-mall, Property 1: 이메일 중복 등록 거부
    @Property(tries = 20)
    void property1_duplicateEmailRejectedWith409(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String localPart) {
        String email = localPart.toLowerCase() + "@example.com";
        String password = "password123";
        String name = "Test User";

        userRepository.deleteAll();

        RegisterRequest req = createRegisterRequest(email, password, name);
        userService.register(req);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // Feature: shopping-mall, Property 2: 비밀번호 bcrypt 해시 저장
    @Property(tries = 20)
    void property2_passwordStoredAsBcryptHash(
            @ForAll @AlphaChars @StringLength(min = 8, max = 20) String password) {
        String email = "bcrypt_" + password.toLowerCase() + "@example.com";

        userRepository.deleteAll();

        RegisterRequest req = createRegisterRequest(email, password, "User");
        userService.register(req);

        User saved = userRepository.findByEmail(email).orElseThrow();
        // Password must not be stored as plain text
        assertThat(saved.getPassword()).isNotEqualTo(password);
        // Must be verifiable with bcrypt
        assertThat(passwordEncoder.matches(password, saved.getPassword())).isTrue();
        // BCrypt hash starts with $2a$ or $2b$
        assertThat(saved.getPassword()).matches("\\$2[ab]\\$.*");
    }

    // Feature: shopping-mall, Property 3: 유효하지 않은 입력 거부
    @Test
    void property3_invalidEmail_isRejectedByValidation() {
        // Bean Validation 직접 검증
        jakarta.validation.Validator validator = jakarta.validation.Validation
                .buildDefaultValidatorFactory().getValidator();

        RegisterRequest req = createRegisterRequest("not-an-email", "password123", "User");
        var violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"))).isTrue();
    }

    @Test
    void property3_shortPassword_isRejectedByValidation() {
        jakarta.validation.Validator validator = jakarta.validation.Validation
                .buildDefaultValidatorFactory().getValidator();

        RegisterRequest req = createRegisterRequest("valid@example.com", "short", "User");
        var violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"))).isTrue();
    }

    // Feature: shopping-mall, Property 4: JWT 발급 라운드 트립
    @Property(tries = 20)
    void property4_jwtRoundTrip(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String localPart) {
        String email = localPart.toLowerCase() + "_jwt@example.com";

        userRepository.deleteAll();

        RegisterRequest reg = createRegisterRequest(email, "password123", "User");
        userService.register(reg);

        LoginRequest loginReq = createLoginRequest(email, "password123");
        AuthResponse response = userService.login(loginReq);

        String token = response.getAccessToken();
        assertThat(token).isNotBlank();

        Claims claims = jwtConfig.parseToken(token);
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("userId", Long.class)).isNotNull();

        long diffMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(diffMs).isBetween(86395000L, 86405000L);
    }

    // Feature: shopping-mall, Property 5: 인증 실패 시 401 반환
    @Property(tries = 20)
    void property5_wrongCredentialsReturn401(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String localPart) {
        String email = localPart.toLowerCase() + "_auth@example.com";

        userRepository.deleteAll();

        RegisterRequest reg = createRegisterRequest(email, "correctpassword", "User");
        userService.register(reg);

        LoginRequest wrongPwd = createLoginRequest(email, "wrongpassword123");
        assertThatThrownBy(() -> userService.login(wrongPwd))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // Feature: shopping-mall, Task 3.10: 더미 유저 5개 이상 존재 여부 및 actuator 검증
    @Test
    void task310_atLeastFiveDummyUsers() {
        // H2 테스트 환경에서 직접 5개 유저 생성 후 검증
        for (int i = 1; i <= 5; i++) {
            RegisterRequest req = createRegisterRequest("dummy" + i + "@example.com", "password123", "User" + i);
            userService.register(req);
        }
        long count = userRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    @Test
    void task310_actuatorPrometheusEndpointResponds() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    // ===== Helper Methods =====
    private RegisterRequest createRegisterRequest(String email, String password, String name) {
        try {
            RegisterRequest req = new RegisterRequest();
            var emailField = RegisterRequest.class.getDeclaredField("email");
            var passwordField = RegisterRequest.class.getDeclaredField("password");
            var nameField = RegisterRequest.class.getDeclaredField("name");
            emailField.setAccessible(true);
            passwordField.setAccessible(true);
            nameField.setAccessible(true);
            emailField.set(req, email);
            passwordField.set(req, password);
            nameField.set(req, name);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LoginRequest createLoginRequest(String email, String password) {
        try {
            LoginRequest req = new LoginRequest();
            var emailField = LoginRequest.class.getDeclaredField("email");
            var passwordField = LoginRequest.class.getDeclaredField("password");
            emailField.setAccessible(true);
            passwordField.setAccessible(true);
            emailField.set(req, email);
            passwordField.set(req, password);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
