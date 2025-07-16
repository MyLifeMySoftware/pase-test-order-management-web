package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Order management with strict role-based access control")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    /**
     * Health check endpoint - Public.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the order management service"
            + " is running (No auth required)")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Order Management service is running", "OK"));
    }

    /**
     * Create new order - Available to all authenticated users.
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.create", description = "Time taken to create order")
    @Operation(
            summary = "Create new order",
            description = "Create a new order for the authenticated user. Available to any authenticated user.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Object>> createOrder(@RequestBody Map<String, Object> orderData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("🛒 Creating order for user: {} with data: {}", currentUsername, orderData);

        Object mockOrder = Map.of(
                "id", 123,
                "username", currentUsername,
                "product", orderData.getOrDefault("product", "Unknown Product"),
                "amount", orderData.getOrDefault("amount", 0.0),
                "status", "PENDING",
                "created_by", currentUsername,
                "message", "Order created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", mockOrder));
    }


    /**
     * Get all orders - ADMIN and MODERATOR only.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.all.get", description = "Time taken to get all orders")
    @Operation(
            summary = "Get all orders",
            description = "Get all orders in the system - Only available to ADMIN and MODERATOR roles",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Object>> getAllOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("📋 Getting all orders requested by: {} with authorities: {}",
                auth.getName(), auth.getAuthorities());

        Object mockOrders = Map.of(
                "requestedBy", auth.getName(),
                "totalOrders", 156,
                "orders", List.of(
                        Map.of("id", 1, "username", "user1", "product",
                                "Product A", "amount", 100.00),
                        Map.of("id", 2, "username", "user2", "product",
                                "Product B", "amount", 250.50),
                        Map.of("id", 3, "username", "user3", "product",
                                "Product C", "amount", 75.25)
                ),
                "page", page,
                "size", size,
                "access_level", "ADMIN_ALL_ORDERS"
        );

        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", mockOrders));
    }
}