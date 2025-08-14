package com.sqli.ecomAnalytics.dto;

import com.sqli.ecomAnalytics.entity.ProductCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductCreateDto {
    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Category is required")
    private ProductCategory category;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotNull (message = "Price is required")
    @Digits(integer = 6, fraction = 2, message = "Invalid number of digits")
    private BigDecimal price;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull (message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
}
