package pase.test.com.order.management.controller;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
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
}