package com.shop.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/simulate")
public class SimulateController {

    @GetMapping("/error")
    public ResponseEntity<Void> simulateError() {
        throw new RuntimeException("Simulated error");
    }

    @GetMapping("/slow")
    public ResponseEntity<String> simulateSlow() throws InterruptedException {
        Thread.sleep(3000);
        return ResponseEntity.ok("slow response");
    }

    @GetMapping("/memory")
    public ResponseEntity<String> simulateMemory() {
        List<byte[]> memoryHog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            memoryHog.add(new byte[1024 * 1024]); // 100MB
        }
        return ResponseEntity.ok("memory allocated: " + memoryHog.size() + "MB");
    }
}
