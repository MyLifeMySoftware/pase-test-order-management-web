package pase.test.com.order.management.service.order;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pase.test.com.database.dto.order.OrderStatusResponse;
import pase.test.com.database.entity.order.OrderStatus;
import pase.test.com.database.exception.order.OrderStatusNotFoundException;
import pase.test.com.database.repository.order.OrderStatusRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;

    @Override
    public List<OrderStatusResponse> getAllActiveStatuses() {
        log.info("Fetching all active order statuses");
        List<OrderStatus> statuses = orderStatusRepository.findAllActiveStatuses();
        return statuses.stream()
                .map(this::convertToOrderStatusResponse)
                .toList();
    }

    @Override
    public OrderStatusResponse getOrderStatusByLabel(String statusLabel) {
        log.info("Fetching order status by label: {}", statusLabel);
        OrderStatus status = orderStatusRepository.findByStatusLabel(statusLabel)
                .orElseThrow(() -> new OrderStatusNotFoundException("Order status not found: " + statusLabel));
        return convertToOrderStatusResponse(status);
    }


    @Override
    public OrderStatus getOrderStatusEntityByLabel(String statusLabel) {
        return orderStatusRepository.findByStatusLabel(statusLabel)
                .orElseThrow(() -> new OrderStatusNotFoundException("Order status not found: " + statusLabel));
    }

    @Transactional
    @Override
    public void initializeDefaultStatuses() {
        log.info("Initializing default order statuses");

        String[] defaultStatuses = {"CREATED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CANCELLED"};

        for (String statusLabel : defaultStatuses) {
            if (!orderStatusRepository.existsByStatusLabel(statusLabel)) {
                OrderStatus status = OrderStatus.builder()
                        .statusLabel(statusLabel)
                        .enabled(true)
                        .deleted(false)
                        .build();
                orderStatusRepository.save(status);
                log.info("Created default order status: {}", statusLabel);
            }
        }
    }

    private OrderStatusResponse convertToOrderStatusResponse(OrderStatus status) {
        return OrderStatusResponse.builder()
                .id(status.getId())
                .statusLabel(status.getStatusLabel())
                .enabled(status.getEnabled())
                .createdOn(status.getCreatedOn())
                .lastUpdated(status.getLastUpdated())
                .build();
    }
}
