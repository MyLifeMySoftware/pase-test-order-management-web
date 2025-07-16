package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.database.dto.order.OrderStatusResponse;
import pase.test.com.order.management.service.order.OrderStatusService;

@Slf4j
@RestController
@RequestMapping("/api/v1/order-statuses")
@RequiredArgsConstructor
@Tag(name = "Order Status", description = "Order status management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.status.list", description = "Time taken to list order statuses")
    @Operation(summary = "Get order statuses", description = "Get list of all active order statuses")
    public ResponseEntity<ApiResponse<List<OrderStatusResponse>>> getAllOrderStatuses() {

        log.info("Getting all order statuses");
        List<OrderStatusResponse> statuses = orderStatusService.getAllActiveStatuses();

        return ResponseEntity.ok(ApiResponse.success("Order statuses retrieved successfully", statuses));
    }
}