package com.sqli.ecomAnalytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class OrderItemsDto {
    @NotNull(message = "Product ID is required")
    private int productId;
    @NotNull(message = "Quantity is required")
    private int quantity;
}
