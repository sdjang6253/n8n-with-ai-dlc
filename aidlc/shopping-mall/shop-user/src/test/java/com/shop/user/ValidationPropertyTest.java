package com.shop.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property 3: 유효하지 않은 입력 거부
 * Validates: Requirements 1.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationPropertyTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // Feature: shopping-mall, Property 3: 유효하지 않은 입력 거부 (이메일 형식)
    @Property(tries = 20)
    void property3_invalidEmailRejectedWith400(@ForAll("invalidEmails") String invalidEmail) {
        Map<String, String> body = Map.of(
            "email", invalidEmail,
            "password", "password123",
            "name", "User"
        );

        try {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Feature: shopping-mall, Property 3: 유효하지 않은 입력 거부 (비밀번호 8자 미만)
    @Property(tries = 20)
    void property3_shortPasswordRejectedWith400(
            @ForAll("shortPasswords") String shortPassword) {
        Map<String, String> body = Map.of(
            "email", "valid@example.com",
            "password", shortPassword,
            "name", "User"
        );

        try {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Provide
    Arbitrary<String> invalidEmails() {
        return Arbitraries.of(
            "notanemail",
            "missing@",
            "@nodomain.com",
            "spaces in@email.com",
            "double@@email.com",
            "no-at-sign",
            "trailing@dot.",
            ""
        );
    }

    @Provide
    Arbitrary<String> shortPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(7);
    }
}
