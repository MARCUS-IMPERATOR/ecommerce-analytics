package com.sqli.ecomAnalytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCreateDto {
    @NotNull(message = "Customer ID is required")
    private int customerId;

    @Builder.Default
    private LocalDateTime orderDate =  LocalDateTime.now();

    @NotNull(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemsDto> orderItems;
}
