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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Order management operations with role-based access control")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    /**
     * Health check endpoint - Public
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the order management service is running")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Order Management service is running", "OK"));
    }

    /**
     * Get user's own orders - Available to all authenticated users
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.my-orders.get", description = "Time taken to get user's orders")
    @Operation(summary = "Get my orders", description = "Get orders for the currently authenticated user")
    public ResponseEntity<ApiResponse<Object>> getMyOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("Getting orders for user: {} with authorities: {}",
                currentUsername, auth.getAuthorities());

        // Simulated response for demonstration
        Object mockOrders = Map.of(
                "username", currentUsername,
                "orders", List.of(
                        Map.of("id", 1, "product", "Sample Product 1", "amount", 100.00, "status", "PENDING"),
                        Map.of("id", 2, "product", "Sample Product 2", "amount", 250.50, "status", "COMPLETED")
                ),
                "authorities", auth.getAuthorities(),
                "page", page,
                "size", size
        );

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", mockOrders));
    }

    /**
     * Create new order - Available to all authenticated users
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.create", description = "Time taken to create order")
    @Operation(summary = "Create new order", description = "Create a new order for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> createOrder(@RequestBody Map<String, Object> orderData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("Creating order for user: {} with data: {}", currentUsername, orderData);

        Object mockOrder = Map.of(
                "id", 123,
                "username", currentUsername,
                "product", orderData.getOrDefault("product", "Unknown Product"),
                "amount", orderData.getOrDefault("amount", 0.0),
                "status", "PENDING",
                "message", "Order created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", mockOrder));
    }

    /**
     * Get specific order - Users can only access their own orders, Admins and Moderators can access all
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.get", description = "Time taken to get specific order")
    @Operation(summary = "Get order by ID", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<Object>> getOrder(@PathVariable Long orderId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdminOrModerator = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") ||
                        authority.getAuthority().equals("ROLE_MODERATOR"));

        log.info("Getting order {} for user: {} (isAdminOrModerator: {})",
                orderId, currentUsername, isAdminOrModerator);

        Object mockOrder = Map.of(
                "id", orderId,
                "username", isAdminOrModerator ? "any-user" : currentUsername,
                "product", "Sample Product",
                "amount", 150.75,
                "status", "PROCESSING",
                "accessLevel", isAdminOrModerator ? "ADMIN_ACCESS" : "USER_ACCESS"
        );

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", mockOrder));
    }

    /**
     * Cancel order - Users can cancel their own orders
     */
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.cancel", description = "Time taken to cancel order")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<ApiResponse<Object>> cancelOrder(@PathVariable Long orderId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        log.info("Cancelling order {} for user: {}", orderId, currentUsername);

        Object result = Map.of(
                "orderId", orderId,
                "status", "CANCELLED",
                "cancelledBy", currentUsername,
                "message", "Order cancelled successfully"
        );

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", result));
    }

    /**
     * Get all orders - ADMIN and MODERATOR only
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.all.get", description = "Time taken to get all orders")
    @Operation(summary = "Get all orders", description = "Get all orders - Admin and Moderator only")
    public ResponseEntity<ApiResponse<Object>> getAllOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Getting all orders requested by: {} with authorities: {}",
                auth.getName(), auth.getAuthorities());

        Object mockOrders = Map.of(
                "requestedBy", auth.getName(),
                "totalOrders", 156,
                "orders", List.of(
                        Map.of("id", 1, "username", "user1", "product", "Product A", "amount", 100.00),
                        Map.of("id", 2, "username", "user2", "product", "Product B", "amount", 250.50),
                        Map.of("id", 3, "username", "user3", "product", "Product C", "amount", 75.25)
                ),
                "page", page,
                "size", size
        );

        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", mockOrders));
    }

    /**
     * Search orders - ADMIN and MODERATOR only
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.search", description = "Time taken to search orders")
    @Operation(summary = "Search orders", description = "Search orders by various criteria - Admin and Moderator only")
    public ResponseEntity<ApiResponse<Object>> searchOrders(
            @Parameter(description = "Search query") @RequestParam String query) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Searching orders with query '{}' requested by: {}", query, auth.getName());

        Object searchResults = Map.of(
                "query", query,
                "searchBy", auth.getName(),
                "results", List.of(
                        Map.of("id", 5, "username", "user5", "product", "Product containing: " + query, "amount", 99.99)
                )
        );

        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", searchResults));
    }

    /**
     * Update order status - ADMIN and MODERATOR only
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.status.update", description = "Time taken to update order status")
    @Operation(summary = "Update order status", description = "Update order status - Admin and Moderator only")
    public ResponseEntity<ApiResponse<Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @Parameter(description = "New status") @RequestParam String status) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Updating order {} status to '{}' requested by: {}",
                orderId, status, auth.getName());

        Object result = Map.of(
                "orderId", orderId,
                "newStatus", status,
                "updatedBy", auth.getName(),
                "timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", result));
    }

    /**
     * Get order statistics - ADMIN only
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "order.statistics", description = "Time taken to get order statistics")
    @Operation(summary = "Get order statistics", description = "Get comprehensive order statistics - Admin only")
    public ResponseEntity<ApiResponse<Object>> getOrderStatistics() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Getting order statistics requested by admin: {}", auth.getName());

        Object statistics = Map.of(
                "totalOrders", 1250,
                "pendingOrders", 45,
                "completedOrders", 1180,
                "cancelledOrders", 25,
                "totalRevenue", 125750.50,
                "averageOrderValue", 100.60,
                "topProducts", List.of("Product A", "Product B", "Product C"),
                "requestedBy", auth.getName()
        );

        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", statistics));
    }

    /**
     * Bulk operations - ADMIN only
     */
    @PostMapping("/admin/bulk-update")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "order.bulk.update", description = "Time taken for bulk operations")
    @Operation(summary = "Bulk update orders", description = "Perform bulk operations on orders - Admin only")
    public ResponseEntity<ApiResponse<Object>> bulkUpdateOrders(
            @RequestBody Map<String, Object> bulkData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Bulk operation requested by admin: {} with data: {}", auth.getName(), bulkData);

        Object result = Map.of(
                "operation", "bulk-update",
                "processedBy", auth.getName(),
                "affectedOrders", bulkData.getOrDefault("orderIds", List.of()),
                "status", "completed"
        );

        return ResponseEntity.ok(ApiResponse.success("Bulk operation completed successfully", result));
    }
}