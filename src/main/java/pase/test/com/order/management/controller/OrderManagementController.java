package pase.test.com.order.management.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pase.test.com.database.dto.ApiResponse;
import pase.test.com.database.dto.order.OrderAssignmentRequest;
import pase.test.com.database.dto.order.OrderCreateRequest;
import pase.test.com.database.dto.order.OrderFilterRequest;
import pase.test.com.database.dto.order.OrderResponse;
import pase.test.com.database.dto.order.OrderUpdateStatusRequest;
import pase.test.com.order.management.service.order.OrderService;


@Slf4j
@RestController
@RequestMapping("/api/v1/order-management")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Complete order lifecycle management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderManagementController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.create", description = "Time taken to create order")
    @Operation(summary = "Create new order", description = "Create a new order in the system")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        log.info("Creating new order from {} to {}", request.getOrigin(), request.getDestination());
        OrderResponse order = orderService.createOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.get.id", description = "Time taken to get order by ID")
    @Operation(summary = "Get order by ID", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable String id) {

        log.info("Getting order by ID: {}", id);
        OrderResponse order = orderService.getOrderById(id);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @GetMapping("/orders/number/{orderNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.get.number", description = "Time taken to get order by number")
    @Operation(summary = "Get order by number", description = "Get order details by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {

        log.info("Getting order by number: {}", orderNumber);
        OrderResponse order = orderService.getOrderByNumber(orderNumber);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    @PostMapping("/orders/list")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.list.filtered", description = "Time taken to list orders with filters")
    @Operation(summary = "List orders with filters",
            description = "List orders with optional filters: status, date range, origin/destination")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listOrdersWithFilters(
            @RequestBody(required = false) OrderFilterRequest filterRequest,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Listing orders with filters: {}", filterRequest);

        if (filterRequest == null) {
            filterRequest = new OrderFilterRequest();
        }

        Page<OrderResponse> orders = orderService.listOrdersWithFilters(filterRequest, pageable);

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.update.status", description = "Time taken to update order status")
    @Operation(summary = "Update order status",
            description = "Change order status (validates valid transitions)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable String id,
            @Valid @RequestBody OrderUpdateStatusRequest request) {

        log.info("Updating order {} status to {}", id, request.getStatusLabel());
        OrderResponse order = orderService.updateOrderStatus(id, request);

        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    @PostMapping("/orders/{id}/assign-driver")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.assign.driver", description = "Time taken to assign driver to order")
    @Operation(summary = "Assign driver to order",
            description = "Assign an active driver to an order (only CREATED orders can be assigned)")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDriverToOrder(
            @Parameter(description = "Order ID") @PathVariable String id,
            @Valid @RequestBody OrderAssignmentRequest request) {

        log.info("Assigning driver {} to order {}", request.getDriverId(), id);
        OrderResponse order = orderService.assignDriverToOrder(id, request);

        return ResponseEntity.ok(ApiResponse.success("Driver assigned to order successfully", order));
    }

    @GetMapping("/drivers/{driverId}/orders")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MODERATOR')")
    @Timed(value = "order.get.by.driver", description = "Time taken to get orders by driver")
    @Operation(summary = "Get orders by driver", description = "Get all orders assigned to a specific driver")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDriver(
            @Parameter(description = "Driver ID") @PathVariable String driverId) {

        log.info("Getting orders for driver: {}", driverId);
        List<OrderResponse> orders = orderService.getOrdersByDriver(driverId);

        return ResponseEntity.ok(ApiResponse.success("Driver orders retrieved successfully", orders));
    }
}