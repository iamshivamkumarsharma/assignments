package org.nbfc.productwa.service;

import org.nbfc.productwa.dto.OrderRequest;
import org.nbfc.productwa.dto.OrderResponse;
import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.model.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse placeOrder(String userEmail, OrderRequest request);

    OrderResponse getById(String userEmail, Long orderId);

    OrderResponse getByIdAsAdmin(Long orderId);

    OrderResponse updateOwnOrder(String userEmail, Long orderId, OrderRequest request);

    void cancelOwnOrder(String userEmail, Long orderId);

    PageResponse<OrderResponse> getMyOrders(String userEmail, Pageable pageable);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    PageResponse<OrderResponse> getByStatus(OrderStatus status, Pageable pageable);

    OrderResponse updateStatus(Long orderId, OrderStatus status);

    void deleteOrder(Long orderId);
}
