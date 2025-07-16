package pase.test.com.order.management.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Implementation Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverService driverService;

    @Mock
    private OrderStatusService orderStatusService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderStatusRepository,
                userRepository,
                driverService,
                orderStatusService
        );
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     Mockito.mockStatic(SecurityContextHolder.class)) {

            String username = "testuser";
            OrderCreateRequest request = createOrderCreateRequest();
            User currentUser = createMockUser(username);
            OrderStatus createdStatus = createMockOrderStatus("CREATED");
            Order savedOrder = createMockOrder("1", "ORD-12345", createdStatus, currentUser);

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
            when(orderStatusService.getOrderStatusEntityByLabel("CREATED")).thenReturn(createdStatus);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            OrderResponse result = orderService.createOrder(request);

            assertThat(result).isNotNull();
            assertThat(result.getOrigin()).isEqualTo(request.getOrigin());
            assertThat(result.getDestination()).isEqualTo(request.getDestination());
            assertThat(result.getDistanceKm()).isEqualTo(request.getDistanceKm());
            assertThat(result.getOrderStatus().getStatusLabel()).isEqualTo("CREATED");

            verify(userRepository).findByUsername(username);
            verify(orderStatusService).getOrderStatusEntityByLabel("CREATED");
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Test
    @DisplayName("Should throw exception when current user not found during order creation")
    void shouldThrowExceptionWhenCurrentUserNotFoundDuringOrderCreation() {
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     Mockito.mockStatic(SecurityContextHolder.class)) {

            String username = "nonexistent";
            OrderCreateRequest request = createOrderCreateRequest();

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(
                    UserNotFoundException.class,
                    () -> orderService.createOrder(request)
            );

            assertThat(exception.getMessage()).isEqualTo("Current user not found: " + username);
            verify(userRepository).findByUsername(username);
        }
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void shouldGetOrderByIdSuccessfully() {
        String orderId = "1";
        Order mockOrder = createMockOrder(orderId, "ORD-12345",
                createMockOrderStatus("CREATED"), createMockUser("testuser"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        OrderResponse result = orderService.getOrderById(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-12345");

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should throw exception when order not found by ID")
    void shouldThrowExceptionWhenOrderNotFoundById() {
        String orderId = "nonexistent";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        assertThat(exception.getMessage()).isEqualTo("Order not found with ID: " + orderId);
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should get order by number successfully")
    void shouldGetOrderByNumberSuccessfully() {
        String orderNumber = "ORD-12345";
        Order mockOrder = createMockOrder("1", orderNumber,
                createMockOrderStatus("CREATED"), createMockUser("testuser"));

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(mockOrder));

        OrderResponse result = orderService.getOrderByNumber(orderNumber);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo(orderNumber);

        verify(orderRepository).findByOrderNumber(orderNumber);
    }

    @Test
    @DisplayName("Should throw exception when order not found by number")
    void shouldThrowExceptionWhenOrderNotFoundByNumber() {
        String orderNumber = "nonexistent";
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderByNumber(orderNumber)
        );

        assertThat(exception.getMessage()).isEqualTo("Order not found with number: " + orderNumber);
        verify(orderRepository).findByOrderNumber(orderNumber);
    }

    @Test
    @DisplayName("Should list orders with filters successfully")
    void shouldListOrdersWithFiltersSuccessfully() {
        OrderFilterRequest filterRequest = OrderFilterRequest.builder()
                .statusLabel("CREATED")
                .location("TestLocation")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        OrderStatus status = createMockOrderStatus("CREATED");
        List<Order> mockOrders = List.of(
                createMockOrder("1", "ORD-1", status, createMockUser("user1")),
                createMockOrder("2", "ORD-2", status, createMockUser("user2"))
        );
        Page<Order> mockPage = new PageImpl<>(mockOrders, pageable, mockOrders.size());

        when(orderStatusService.getOrderStatusEntityByLabel("CREATED")).thenReturn(status);
        when(orderRepository.findWithFilters(eq(status), eq(filterRequest.getStartDate()),
                eq(filterRequest.getEndDate()), eq(filterRequest.getLocation()), eq(pageable)))
                .thenReturn(mockPage);

        Page<OrderResponse> result = orderService.listOrdersWithFilters(filterRequest, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(orderStatusService).getOrderStatusEntityByLabel("CREATED");
        verify(orderRepository).findWithFilters(eq(status), eq(filterRequest.getStartDate()),
                eq(filterRequest.getEndDate()), eq(filterRequest.getLocation()), eq(pageable));
    }

    @Test
    @DisplayName("Should list orders without status filter")
    void shouldListOrdersWithoutStatusFilter() {
        OrderFilterRequest filterRequest = OrderFilterRequest.builder()
                .location("TestLocation")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> mockOrders = List.of(createMockOrder("1", "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1")));
        Page<Order> mockPage = new PageImpl<>(mockOrders, pageable, mockOrders.size());

        when(orderRepository.findWithFilters(eq(null), eq(filterRequest.getStartDate()),
                eq(filterRequest.getEndDate()), eq(filterRequest.getLocation()), eq(pageable)))
                .thenReturn(mockPage);

        Page<OrderResponse> result = orderService.listOrdersWithFilters(filterRequest, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(orderRepository).findWithFilters(eq(null), eq(filterRequest.getStartDate()),
                eq(filterRequest.getEndDate()), eq(filterRequest.getLocation()), eq(pageable));
    }

    @Test
    @DisplayName("Should update order status successfully")
    void shouldUpdateOrderStatusSuccessfully() {
        String orderId = "1";
        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .statusLabel("ASSIGNED")
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        OrderStatus newStatus = createMockOrderStatus("ASSIGNED");
        Order updatedOrder = createMockOrder(orderId, "ORD-1", newStatus, createMockUser("user1"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderStatusService.getOrderStatusEntityByLabel("ASSIGNED")).thenReturn(newStatus);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderResponse result = orderService.updateOrderStatus(orderId, request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus().getStatusLabel()).isEqualTo("ASSIGNED");

        verify(orderRepository).findById(orderId);
        verify(orderStatusService).getOrderStatusEntityByLabel("ASSIGNED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        String orderId = "1";
        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .statusLabel("CREATED")
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("DELIVERED"), createMockUser("user1"));
        OrderStatus newStatus = createMockOrderStatus("CREATED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderStatusService.getOrderStatusEntityByLabel("CREATED")).thenReturn(newStatus);

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> orderService.updateOrderStatus(orderId, request)
        );

        assertThat(exception.getMessage()).contains("Invalid status transition from DELIVERED to CREATED");

        verify(orderRepository).findById(orderId);
        verify(orderStatusService).getOrderStatusEntityByLabel("CREATED");
    }

    @Test
    @DisplayName("Should assign driver to order successfully")
    void shouldAssignDriverToOrderSuccessfully() {
        String orderId = "1";
        String driverId = "driver1";
        OrderAssignmentRequest request = OrderAssignmentRequest.builder()
                .driverId(driverId)
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        Driver mockDriver = createMockDriver(driverId, "Driver1", true);
        OrderStatus assignedStatus = createMockOrderStatus("ASSIGNED");
        Order updatedOrder = createMockOrder(orderId, "ORD-1",
                assignedStatus, createMockUser("user1"));
        updatedOrder.setDriver(mockDriver);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(driverService.getDriverEntityById(driverId)).thenReturn(mockDriver);
        when(orderStatusService.getOrderStatusEntityByLabel("ASSIGNED")).thenReturn(assignedStatus);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderResponse result = orderService.assignDriverToOrder(orderId, request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus().getStatusLabel()).isEqualTo("ASSIGNED");
        assertThat(result.getDriver()).isNotNull();
        assertThat(result.getDriver().getId()).isEqualTo(driverId);

        verify(orderRepository).findById(orderId);
        verify(driverService).getDriverEntityById(driverId);
        verify(orderStatusService).getOrderStatusEntityByLabel("ASSIGNED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when assigning driver to non-created order")
    void shouldThrowExceptionWhenAssigningDriverToNonCreatedOrder() {
        String orderId = "1";
        String driverId = "driver1";
        OrderAssignmentRequest request = OrderAssignmentRequest.builder()
                .driverId(driverId)
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("ASSIGNED"), createMockUser("user1"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        InvalidOrderStatusTransitionException exception = assertThrows(
                InvalidOrderStatusTransitionException.class,
                () -> orderService.assignDriverToOrder(orderId, request)
        );

        assertThat(exception.getMessage())
                .contains("Cannot assign driver to order. Order must be in CREATED status, but is: ASSIGNED");

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should throw exception when assigning inactive driver")
    void shouldThrowExceptionWhenAssigningInactiveDriver() {
        String orderId = "1";
        String driverId = "driver1";
        OrderAssignmentRequest request = OrderAssignmentRequest.builder()
                .driverId(driverId)
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        Driver inactiveDriver = createMockDriver(driverId, "Driver1", false);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(driverService.getDriverEntityById(driverId)).thenReturn(inactiveDriver);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.assignDriverToOrder(orderId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Cannot assign inactive driver to order");

        verify(orderRepository).findById(orderId);
        verify(driverService).getDriverEntityById(driverId);
    }

    @Test
    @DisplayName("Should add attachment to order successfully")
    void shouldAddAttachmentToOrderSuccessfully() {
        String orderId = "1";
        AssignmentAttachment attachment = createMockAttachment();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        Order updatedOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        updatedOrder.setAssignmentAttachment(attachment);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderResponse result = orderService.addAttachmentToOrder(orderId, attachment);

        assertThat(result).isNotNull();
        assertThat(result.getAssignmentAttachment()).isNotNull();

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should get orders by driver successfully")
    void shouldGetOrdersByDriverSuccessfully() {
        String driverId = "driver1";
        Driver mockDriver = createMockDriver(driverId, "Driver1", true);
        List<Order> mockOrders = List.of(
                createMockOrder("1", "ORD-1",
                        createMockOrderStatus("ASSIGNED"), createMockUser("user1")),
                createMockOrder("2", "ORD-2",
                        createMockOrderStatus("IN_TRANSIT"), createMockUser("user2"))
        );

        when(driverService.getDriverEntityById(driverId)).thenReturn(mockDriver);
        when(orderRepository.findByDriver(mockDriver)).thenReturn(mockOrders);

        List<OrderResponse> result = orderService.getOrdersByDriver(driverId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(driverService).getDriverEntityById(driverId);
        verify(orderRepository).findByDriver(mockDriver);
    }

    @Test
    @DisplayName("Should allow transition to CANCELLED from any status")
    void shouldAllowTransitionToCancelledFromAnyStatus() {
        String orderId = "1";
        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .statusLabel("CANCELLED")
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("IN_TRANSIT"), createMockUser("user1"));
        OrderStatus cancelledStatus = createMockOrderStatus("CANCELLED");
        Order updatedOrder = createMockOrder(orderId, "ORD-1", cancelledStatus,
                createMockUser("user1"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderStatusService.getOrderStatusEntityByLabel("CANCELLED")).thenReturn(cancelledStatus);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderResponse result = orderService.updateOrderStatus(orderId, request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus().getStatusLabel()).isEqualTo("CANCELLED");

        verify(orderRepository).findById(orderId);
        verify(orderStatusService).getOrderStatusEntityByLabel("CANCELLED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should allow same status transition")
    void shouldAllowSameStatusTransition() {
        String orderId = "1";
        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .statusLabel("CREATED")
                .build();
        Order mockOrder = createMockOrder(orderId, "ORD-1",
                createMockOrderStatus("CREATED"), createMockUser("user1"));
        OrderStatus sameStatus = createMockOrderStatus("CREATED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderStatusService.getOrderStatusEntityByLabel("CREATED")).thenReturn(sameStatus);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponse result = orderService.updateOrderStatus(orderId, request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus().getStatusLabel()).isEqualTo("CREATED");

        verify(orderRepository).findById(orderId);
        verify(orderStatusService).getOrderStatusEntityByLabel("CREATED");
        verify(orderRepository).save(any(Order.class));
    }

    private OrderCreateRequest createOrderCreateRequest() {
        return OrderCreateRequest.builder()
                .origin("Origin City")
                .destination("Destination City")
                .distanceKm(100D)
                .estimatedDurationMinutes(120)
                .build();
    }

    private User createMockUser(String username) {
        return User.builder()
                .id(1L)
                .username(username)
                .email(username + "@test.com")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    private OrderStatus createMockOrderStatus(String label) {
        return OrderStatus.builder()
                .id("status-" + label.toLowerCase())
                .statusLabel(label)
                .enabled(true)
                .build();
    }

    private Order createMockOrder(String id, String orderNumber, OrderStatus status, User user) {
        return Order.builder()
                .id(id)
                .orderNumber(orderNumber)
                .origin("Origin City")
                .destination("Destination City")
                .distanceKm(100D)
                .estimatedDurationMinutes(120)
                .orderStatus(status)
                .createdByUser(user)
                .enabled(true)
                .deleted(false)
                .createdOn(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private Driver createMockDriver(String id, String name, boolean enabled) {
        return Driver.builder()
                .id(id)
                .driverName(name)
                .licenseNumber("LIC-" + id)
                .phoneNumber("+1234567890")
                .email(name.toLowerCase() + "@test.com")
                .enabled(enabled)
                .build();
    }

    private AssignmentAttachment createMockAttachment() {
        return AssignmentAttachment.builder()
                .id("attachment-1")
                .fileName("test-file.pdf")
                .filePath("/uploads/test-file.pdf")
                .fileSizeBytes(1024L)
                .build();
    }
}