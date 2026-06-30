package org.nbfc.productwa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private LocalDateTime orderDate;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private Long productId;
    private String productName;
    private Long userId;
    private String customerName;
}
