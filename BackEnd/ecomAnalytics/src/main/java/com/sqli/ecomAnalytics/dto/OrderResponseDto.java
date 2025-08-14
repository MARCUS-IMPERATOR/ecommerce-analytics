package com.sqli.ecomAnalytics.dto;

import com.sqli.ecomAnalytics.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderResponseDto {
    private int orderId;
    private int customerId;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private List<OrderItemsResponseDto> orderItems;
}
