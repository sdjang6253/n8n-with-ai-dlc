package com.shop.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulate")
public class SimulateController {

    @GetMapping("/error")
    public ResponseEntity<Void> simulateError() {
        throw new RuntimeException("Simulated error");
    }

    @GetMapping("/slow")
    public ResponseEntity<String> simulateSlow() throws InterruptedException {
        Thread.sleep(3500);
        return ResponseEntity.ok("slow response");
    }

    @GetMapping("/memory")
    public ResponseEntity<String> simulateMemory() {
        byte[] data = new byte[100 * 1024 * 1024]; // 100MB
        return ResponseEntity.ok("allocated " + data.length + " bytes");
    }
}
