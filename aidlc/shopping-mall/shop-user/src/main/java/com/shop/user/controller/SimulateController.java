package com.shop.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/simulate")
public class SimulateController {

    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> simulateError() {
        throw new RuntimeException("Simulated error for testing");
    }

    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> simulateSlow() throws InterruptedException {
        Thread.sleep(3500);
        return ResponseEntity.ok(Map.of("message", "Slow response completed", "delayMs", 3500));
    }

    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> simulateMemory() {
        // Allocate ~100MB to increase heap usage
        byte[] memoryHog = new byte[100 * 1024 * 1024];
        long allocated = memoryHog.length;
        return ResponseEntity.ok(Map.of(
            "message", "Memory allocated",
            "allocatedBytes", allocated
        ));
    }
}
