package pase.test.com.order.management.service.order;

import static pase.test.com.database.enums.OrderStatusEnum.ASSIGNED;
import static pase.test.com.database.enums.OrderStatusEnum.CANCELLED;
import static pase.test.com.database.enums.OrderStatusEnum.CREATED;
import static pase.test.com.database.enums.OrderStatusEnum.DELIVERED;
import static pase.test.com.database.enums.OrderStatusEnum.IN_TRANSIT;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.dto.driver.DriverResponse;
import pase.test.com.database.dto.order.OrderAssignmentRequest;
import pase.test.com.database.dto.order.OrderCreateRequest;
import pase.test.com.database.dto.order.OrderFilterRequest;
import pase.test.com.database.dto.order.OrderResponse;
import pase.test.com.database.dto.order.OrderUpdateStatusRequest;
import pase.test.com.database.entity.driver.Driver;
import pase.test.com.database.entity.order.Order;
import pase.test.com.database.entity.order.OrderStatus;
import pase.test.com.database.entity.order.attachment.AssignmentAttachment;
import pase.test.com.database.entity.user.User;
import pase.test.com.database.exception.auth.UserNotFoundException;
import pase.test.com.database.exception.order.InvalidOrderStatusTransitionException;
import pase.test.com.database.exception.order.OrderNotFoundException;
import pase.test.com.database.repository.order.OrderRepository;
import pase.test.com.database.repository.order.OrderStatusRepository;
import pase.test.com.database.repository.user.UserRepository;
import pase.test.com.order.management.service.driver.DriverService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;
    private final DriverService driverService;
    private final OrderStatusService orderStatusService;

    private static final List<String> VALID_STATUS_FLOW = List.of(
            CREATED.toString(),
            ASSIGNED.toString(),
            IN_TRANSIT.toString(),
            DELIVERED.toString(),
            CANCELLED.toString()
    );

    @Transactional
    @Override
    public OrderResponse createOrder(OrderCreateRequest request) {
        log.info("Creating new order from {} to {}", request.getOrigin(), request.getDestination());

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found: " + currentUsername));

        OrderStatus createdStatus = orderStatusService.getOrderStatusEntityByLabel("CREATED");

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .orderStatus(createdStatus)
                .createdByUser(currentUser)
                .enabled(true)
                .deleted(false)
                .build();

        order = orderRepository.save(order);
        log.info("Order created successfully: {}", order.getOrderNumber());

        return convertToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderById(String id) {
        log.info("Fetching order by ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        return convertToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.info("Fetching order by number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with number: " + orderNumber));
        return convertToOrderResponse(order);
    }

    @Override
    public Page<OrderResponse> listOrdersWithFilters(OrderFilterRequest filterRequest, Pageable pageable) {
        log.info("Listing orders with filters: {}", filterRequest);

        OrderStatus status = null;
        if (filterRequest.getStatusLabel() != null) {
            status = orderStatusService.getOrderStatusEntityByLabel(filterRequest.getStatusLabel());
        }

        Page<Order> orders = orderRepository.findWithFilters(
                status,
                filterRequest.getStartDate(),
                filterRequest.getEndDate(),
                filterRequest.getLocation(),
                pageable
        );

        return orders.map(this::convertToOrderResponse);
    }

    @Transactional
    @Override
    public OrderResponse updateOrderStatus(String id, OrderUpdateStatusRequest request) {
        log.info("Updating order {} status to {}", id, request.getStatusLabel());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        OrderStatus newStatus = orderStatusService.getOrderStatusEntityByLabel(request.getStatusLabel());

        // Validate status transition
        validateStatusTransition(order.getOrderStatus().getStatusLabel(), newStatus.getStatusLabel());

        order.setOrderStatus(newStatus);
        order = orderRepository.save(order);

        log.info("Order status updated successfully: {} -> {}", id, request.getStatusLabel());
        return convertToOrderResponse(order);
    }

    @Transactional
    @Override
    public OrderResponse assignDriverToOrder(String orderId, OrderAssignmentRequest request) {
        log.info("Assigning driver {} to order {}", request.getDriverId(), orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Validate order status (only CREATED orders can be assigned)
        if (!"CREATED".equals(order.getOrderStatus().getStatusLabel())) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot assign driver to order. Order must be in CREATED status, but is: "
                            + order.getOrderStatus().getStatusLabel());
        }

        // Get and validate driver
        Driver driver = driverService.getDriverEntityById(request.getDriverId());

        if (Boolean.FALSE.equals(driver.getEnabled())) {
            throw new IllegalArgumentException("Cannot assign inactive driver to order");
        }

        // Assign driver and update status to ASSIGNED
        order.setDriver(driver);
        OrderStatus assignedStatus = orderStatusService.getOrderStatusEntityByLabel("ASSIGNED");
        order.setOrderStatus(assignedStatus);

        order = orderRepository.save(order);
        log.info("Driver assigned successfully to order: {}", orderId);

        return convertToOrderResponse(order);
    }

    @Transactional
    @Override
    public OrderResponse addAttachmentToOrder(String orderId, AssignmentAttachment attachment) {
        log.info("Adding attachment to order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        order.setAssignmentAttachment(attachment);
        order = orderRepository.save(order);

        log.info("Attachment added successfully to order: {}", orderId);
        return convertToOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByDriver(String driverId) {
        log.info("Fetching orders for driver: {}", driverId);

        Driver driver = driverService.getDriverEntityById(driverId);
        List<Order> orders = orderRepository.findByDriver(driver);

        return orders.stream()
                .map(this::convertToOrderResponse)
                .toList();
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp + "-" + uuid;
    }


    private void validateStatusTransition(String currentStatus, String newStatus) {
        int currentIndex = VALID_STATUS_FLOW.indexOf(currentStatus);
        int newIndex = VALID_STATUS_FLOW.indexOf(newStatus);

        if (currentIndex == -1 || newIndex == -1) {
            throw new InvalidOrderStatusTransitionException("Invalid status: " + currentStatus + " -> " + newStatus);
        }

        if (currentIndex == newIndex) {
            return;
        }

        if (newIndex > currentIndex || "CANCELLED".equals(newStatus)) {
            return;
        }

        throw new InvalidOrderStatusTransitionException(
                "Invalid status transition from " + currentStatus + " to " + newStatus);
    }

    private OrderResponse convertToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .origin(order.getOrigin())
                .destination(order.getDestination())
                .distanceKm(order.getDistanceKm())
                .estimatedDurationMinutes(order.getEstimatedDurationMinutes())
                .orderStatus(order.getOrderStatus() != null
                        ? OrderResponse.OrderStatusInfo.builder()
                        .id(order.getOrderStatus().getId())
                        .statusLabel(order.getOrderStatus().getStatusLabel())
                        .build() : null)
                .driver(order.getDriver() != null
                        ? DriverResponse.builder()
                        .id(order.getDriver().getId())
                        .driverName(order.getDriver().getDriverName())
                        .licenseNumber(order.getDriver().getLicenseNumber())
                        .phoneNumber(order.getDriver().getPhoneNumber())
                        .email(order.getDriver().getEmail())
                        .enabled(order.getDriver().getEnabled())
                        .build() : null)
                .assignmentAttachment(order.getAssignmentAttachment() != null
                        ? OrderResponse.AttachmentInfo.builder()
                        .id(order.getAssignmentAttachment().getId())
                        .fileName(order.getAssignmentAttachment().getFileName())
                        .filePath(order.getAssignmentAttachment().getFilePath())
                        .fileSizeBytes(order.getAssignmentAttachment().getFileSizeBytes())
                        .attachmentType(order.getAssignmentAttachment().getAttachmentType() != null
                                ? OrderResponse.AttachmentTypeInfo.builder()
                                .id(order.getAssignmentAttachment().getAttachmentType().getId())
                                .typeLabel(order.getAssignmentAttachment().getAttachmentType().getTypeLabel())
                                .allowedExtensions(
                                        order.getAssignmentAttachment().getAttachmentType().getAllowedExtensions()
                                )
                                .build() : null)
                        .build() : null)
                .createdByUser(order.getCreatedByUser() != null
                        ? OrderResponse.UserInfo.builder()
                        .id(order.getCreatedByUser().getId())
                        .username(order.getCreatedByUser().getUsername())
                        .fullName(order.getCreatedByUser().getFullName())
                        .build() : null)
                .createdOn(order.getCreatedOn())
                .lastUpdated(order.getLastUpdated())
                .modifiedBy(order.getModifiedBy())
                .build();
    }
}
