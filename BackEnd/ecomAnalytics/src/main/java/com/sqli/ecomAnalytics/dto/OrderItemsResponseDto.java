package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderItemsResponseDto {
    private int orderId;
    private int productId;
    private int quantity;
    private String productName;
    private BigDecimal unitPrice;
}
