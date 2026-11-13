package com.borae.system;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/status")
    public SystemStatus status() {
        return new SystemStatus("UP", "Boraê API", Instant.now());
    }

    public record SystemStatus(String status, String service, Instant timestamp) {
    }
}
