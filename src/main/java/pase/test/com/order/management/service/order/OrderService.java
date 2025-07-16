package pase.test.com.order.management.service.order;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pase.test.com.database.dto.order.OrderAssignmentRequest;
import pase.test.com.database.dto.order.OrderCreateRequest;
import pase.test.com.database.dto.order.OrderFilterRequest;
import pase.test.com.database.dto.order.OrderResponse;
import pase.test.com.database.dto.order.OrderUpdateStatusRequest;
import pase.test.com.database.entity.order.attachment.AssignmentAttachment;

public interface OrderService {

    OrderResponse createOrder(OrderCreateRequest request);

    OrderResponse getOrderById(String id);

    OrderResponse getOrderByNumber(String orderNumber);

    Page<OrderResponse> listOrdersWithFilters(OrderFilterRequest filterRequest, Pageable pageable);

    OrderResponse updateOrderStatus(String id, OrderUpdateStatusRequest request);

    OrderResponse assignDriverToOrder(String orderId, OrderAssignmentRequest request);

    OrderResponse addAttachmentToOrder(String orderId, AssignmentAttachment attachment);

    List<OrderResponse> getOrdersByDriver(String driverId);

}
