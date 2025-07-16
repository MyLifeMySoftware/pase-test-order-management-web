package pase.test.com.order.management.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.mockito.junit.jupiter.MockitoExtension;
import pase.test.com.database.dto.order.OrderStatusResponse;
import pase.test.com.database.entity.order.OrderStatus;
import pase.test.com.database.exception.order.OrderStatusNotFoundException;
import pase.test.com.database.repository.order.OrderStatusRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Status Service Implementation Tests")
class OrderStatusServiceImplTest {

    @Mock
    private OrderStatusRepository orderStatusRepository;

    private OrderStatusServiceImpl orderStatusService;

    @BeforeEach
    void setUp() {
        orderStatusService = new OrderStatusServiceImpl(orderStatusRepository);
    }

    @Test
    @DisplayName("Should get all active statuses successfully")
    void shouldGetAllActiveStatusesSuccessfully() {
        List<OrderStatus> mockStatuses = List.of(
                createMockOrderStatus("1", "CREATED", true),
                createMockOrderStatus("2", "ASSIGNED", true),
                createMockOrderStatus("3", "IN_PROGRESS", true)
        );

        when(orderStatusRepository.findAllActiveStatuses()).thenReturn(mockStatuses);

        List<OrderStatusResponse> result = orderStatusService.getAllActiveStatuses();

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(OrderStatusResponse::getStatusLabel)
                .containsExactly("CREATED", "ASSIGNED", "IN_PROGRESS");

        verify(orderStatusRepository).findAllActiveStatuses();
    }

    @Test
    @DisplayName("Should return empty list when no active statuses exist")
    void shouldReturnEmptyListWhenNoActiveStatusesExist() {
        when(orderStatusRepository.findAllActiveStatuses()).thenReturn(List.of());

        List<OrderStatusResponse> result = orderStatusService.getAllActiveStatuses();

        assertThat(result).isEmpty();
        verify(orderStatusRepository).findAllActiveStatuses();
    }

    @Test
    @DisplayName("Should get order status by label successfully")
    void shouldGetOrderStatusByLabelSuccessfully() {
        String statusLabel = "CREATED";
        OrderStatus mockStatus = createMockOrderStatus("1", statusLabel, true);

        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.of(mockStatus));

        OrderStatusResponse result = orderStatusService.getOrderStatusByLabel(statusLabel);

        assertThat(result).isNotNull();
        assertThat(result.getStatusLabel()).isEqualTo(statusLabel);
        assertThat(result.getId()).isEqualTo("1");
        assertThat(result.getEnabled()).isTrue();

        verify(orderStatusRepository).findByStatusLabel(statusLabel);
    }

    @Test
    @DisplayName("Should throw exception when order status not found by label")
    void shouldThrowExceptionWhenOrderStatusNotFoundByLabel() {
        String statusLabel = "NONEXISTENT";
        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusByLabel(statusLabel)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: " + statusLabel);
        verify(orderStatusRepository).findByStatusLabel(statusLabel);
    }

    @Test
    @DisplayName("Should get order status entity by label successfully")
    void shouldGetOrderStatusEntityByLabelSuccessfully() {
        String statusLabel = "ASSIGNED";
        OrderStatus mockStatus = createMockOrderStatus("2", statusLabel, true);

        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.of(mockStatus));

        OrderStatus result = orderStatusService.getOrderStatusEntityByLabel(statusLabel);

        assertThat(result).isNotNull();
        assertThat(result.getStatusLabel()).isEqualTo(statusLabel);
        assertThat(result.getId()).isEqualTo("2");

        verify(orderStatusRepository).findByStatusLabel(statusLabel);
    }

    @Test
    @DisplayName("Should throw exception when order status entity not found by label")
    void shouldThrowExceptionWhenOrderStatusEntityNotFoundByLabel() {
        String statusLabel = "INVALID";
        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusEntityByLabel(statusLabel)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: " + statusLabel);
        verify(orderStatusRepository).findByStatusLabel(statusLabel);
    }

    @Test
    @DisplayName("Should initialize default statuses when none exist")
    void shouldInitializeDefaultStatusesWhenNoneExist() {
        when(orderStatusRepository.existsByStatusLabel("CREATED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("ASSIGNED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("IN_PROGRESS")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("COMPLETED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("CANCELLED")).thenReturn(false);

        OrderStatus savedStatus = createMockOrderStatus("1", "CREATED", true);
        when(orderStatusRepository.save(any(OrderStatus.class))).thenReturn(savedStatus);

        orderStatusService.initializeDefaultStatuses();

        verify(orderStatusRepository).existsByStatusLabel("CREATED");
        verify(orderStatusRepository).existsByStatusLabel("ASSIGNED");
        verify(orderStatusRepository).existsByStatusLabel("IN_PROGRESS");
        verify(orderStatusRepository).existsByStatusLabel("COMPLETED");
        verify(orderStatusRepository).existsByStatusLabel("CANCELLED");
        verify(orderStatusRepository, org.mockito.Mockito.times(5)).save(any(OrderStatus.class));
    }

    @Test
    @DisplayName("Should skip creating existing statuses during initialization")
    void shouldSkipCreatingExistingStatusesDuringInitialization() {
        when(orderStatusRepository.existsByStatusLabel("CREATED")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("ASSIGNED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("IN_PROGRESS")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("COMPLETED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("CANCELLED")).thenReturn(true);

        OrderStatus savedStatus = createMockOrderStatus("2", "ASSIGNED", true);
        when(orderStatusRepository.save(any(OrderStatus.class))).thenReturn(savedStatus);

        orderStatusService.initializeDefaultStatuses();

        verify(orderStatusRepository, org.mockito.Mockito.times(2)).save(any(OrderStatus.class));
    }

    @Test
    @DisplayName("Should not save any statuses when all exist")
    void shouldNotSaveAnyStatusesWhenAllExist() {
        when(orderStatusRepository.existsByStatusLabel("CREATED")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("ASSIGNED")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("IN_PROGRESS")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("COMPLETED")).thenReturn(true);
        when(orderStatusRepository.existsByStatusLabel("CANCELLED")).thenReturn(true);

        orderStatusService.initializeDefaultStatuses();

        verify(orderStatusRepository, never()).save(any(OrderStatus.class));
    }

    @Test
    @DisplayName("Should handle null status label gracefully")
    void shouldHandleNullStatusLabelGracefully() {
        when(orderStatusRepository.findByStatusLabel(null)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusByLabel(null)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: null");
        verify(orderStatusRepository).findByStatusLabel(null);
    }

    @Test
    @DisplayName("Should handle empty status label")
    void shouldHandleEmptyStatusLabel() {
        String emptyLabel = "";
        when(orderStatusRepository.findByStatusLabel(emptyLabel)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusByLabel(emptyLabel)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: ");
        verify(orderStatusRepository).findByStatusLabel(emptyLabel);
    }

    @Test
    @DisplayName("Should handle status label with whitespace")
    void shouldHandleStatusLabelWithWhitespace() {
        String labelWithSpaces = "  CREATED  ";
        when(orderStatusRepository.findByStatusLabel(labelWithSpaces)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusByLabel(labelWithSpaces)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: " + labelWithSpaces);
        verify(orderStatusRepository).findByStatusLabel(labelWithSpaces);
    }

    @Test
    @DisplayName("Should convert OrderStatus to OrderStatusResponse correctly")
    void shouldConvertOrderStatusToOrderStatusResponseCorrectly() {
        String statusLabel = "COMPLETED";
        OrderStatus mockStatus = createMockOrderStatus("3", statusLabel, true);
        mockStatus.setCreatedOn(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        mockStatus.setLastUpdated(LocalDateTime.of(2024, 1, 2, 15, 30, 0));

        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.of(mockStatus));

        OrderStatusResponse result = orderStatusService.getOrderStatusByLabel(statusLabel);

        assertThat(result.getId()).isEqualTo("3");
        assertThat(result.getStatusLabel()).isEqualTo(statusLabel);
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getCreatedOn()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(result.getLastUpdated()).isEqualTo(LocalDateTime.of(2024, 1, 2, 15, 30, 0));
    }

    @Test
    @DisplayName("Should handle disabled status correctly")
    void shouldHandleDisabledStatusCorrectly() {
        String statusLabel = "DISABLED_STATUS";
        OrderStatus mockStatus = createMockOrderStatus("4", statusLabel, false);

        when(orderStatusRepository.findByStatusLabel(statusLabel)).thenReturn(Optional.of(mockStatus));

        OrderStatusResponse result = orderStatusService.getOrderStatusByLabel(statusLabel);

        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getStatusLabel()).isEqualTo(statusLabel);

        verify(orderStatusRepository).findByStatusLabel(statusLabel);
    }

    @Test
    @DisplayName("Should handle case sensitive status labels")
    void shouldHandleCaseSensitiveStatusLabels() {
        String lowerCaseLabel = "created";
        when(orderStatusRepository.findByStatusLabel(lowerCaseLabel)).thenReturn(Optional.empty());

        OrderStatusNotFoundException exception = assertThrows(
                OrderStatusNotFoundException.class,
                () -> orderStatusService.getOrderStatusByLabel(lowerCaseLabel)
        );

        assertThat(exception.getMessage()).isEqualTo("Order status not found: " + lowerCaseLabel);
        verify(orderStatusRepository).findByStatusLabel(lowerCaseLabel);
    }

    @Test
    @DisplayName("Should initialize statuses in correct order")
    void shouldInitializeStatusesInCorrectOrder() {
        when(orderStatusRepository.existsByStatusLabel("CREATED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("ASSIGNED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("IN_PROGRESS")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("COMPLETED")).thenReturn(false);
        when(orderStatusRepository.existsByStatusLabel("CANCELLED")).thenReturn(false);

        OrderStatus savedStatus = createMockOrderStatus("1", "CREATED", true);
        when(orderStatusRepository.save(any(OrderStatus.class))).thenReturn(savedStatus);

        orderStatusService.initializeDefaultStatuses();

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(orderStatusRepository);
        inOrder.verify(orderStatusRepository).existsByStatusLabel("CREATED");
        inOrder.verify(orderStatusRepository).existsByStatusLabel("ASSIGNED");
        inOrder.verify(orderStatusRepository).existsByStatusLabel("IN_PROGRESS");
        inOrder.verify(orderStatusRepository).existsByStatusLabel("COMPLETED");
        inOrder.verify(orderStatusRepository).existsByStatusLabel("CANCELLED");
    }

    private OrderStatus createMockOrderStatus(String id, String statusLabel, boolean enabled) {
        return OrderStatus.builder()
                .id(id)
                .statusLabel(statusLabel)
                .enabled(enabled)
                .deleted(false)
                .createdOn(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}