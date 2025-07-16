package pase.test.com.order.management.service.order;

import java.util.List;
import pase.test.com.database.dto.order.OrderStatusResponse;
import pase.test.com.database.entity.order.OrderStatus;

public interface OrderStatusService {

    List<OrderStatusResponse> getAllActiveStatuses();

    OrderStatusResponse getOrderStatusByLabel(String statusLabel);

    OrderStatus getOrderStatusEntityByLabel(String statusLabel);

    void initializeDefaultStatuses();
}
