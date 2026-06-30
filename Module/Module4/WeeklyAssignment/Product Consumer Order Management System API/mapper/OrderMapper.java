package org.nbfc.productwa.mapper;

import org.nbfc.productwa.dto.OrderResponse;
import org.nbfc.productwa.model.Order;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between {@link Order} entity and its response DTO.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .productId(order.getProduct().getId())
                .productName(order.getProduct().getProductName())
                .userId(order.getUser().getId())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .build();
    }
}
