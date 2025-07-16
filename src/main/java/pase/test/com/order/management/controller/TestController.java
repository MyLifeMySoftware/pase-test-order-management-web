package pase.test.com.order.management.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test", description = "Services to test authentication and authorization")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Object>> publicEndpoint() {
        Object data = Map.of(
                "message", "This is a PUBLIC endpoint",
                "timestamp", LocalDateTime.now(),
                "authentication", "NOT REQUIRED",
                "service", "Management Service"
        );

        return ResponseEntity.ok(ApiResponse.success("Public endpoint working", data));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Management Service is RUNNING - No Auth Required");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<ApiResponse<Object>> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Object dashboardData = Map.of(
                "user", auth.getName(),
                "authorities", auth.getAuthorities(),
                "timestamp", LocalDateTime.now(),
                "service", "Management Service",
                "version", "1.0.0"
        );

        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboardData));
    }

    @GetMapping("/system/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getSystemInfo() {

        Object systemInfo = Map.of(
                "service", "Pase Management Service",
                "version", "1.0.0",
                "java.version", System.getProperty("java.version"),
                "spring.version", org.springframework.core.SpringVersion.getVersion(),
                "timestamp", LocalDateTime.now(),
                "status", "RUNNING"
        );

        return ResponseEntity.ok(ApiResponse.success("System information retrieved", systemInfo));
    }

    @GetMapping("/test/auth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<ApiResponse<Object>> testAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Object authData = Map.of(
                "authenticated", auth.isAuthenticated(),
                "username", auth.getName(),
                "authorities", auth.getAuthorities(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication test successful", authData));
    }
}