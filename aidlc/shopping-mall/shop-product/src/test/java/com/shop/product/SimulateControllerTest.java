package com.shop.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Feature: shopping-mall, Property 19: 알람 시뮬레이션 엔드포인트 동작
// /simulate/error → HTTP 500, /simulate/slow → 3000ms 이상 지연
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimulateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void property19_simulateError_returns500() throws Exception {
        mockMvc.perform(get("/simulate/error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void property19_simulateSlow_takesAtLeast3Seconds() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/simulate/slow"))
                .andExpect(status().isOk());
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(3000L);
    }

    @Test
    void property19_simulateMemory_returns200() throws Exception {
        mockMvc.perform(get("/simulate/memory"))
                .andExpect(status().isOk());
    }
}
