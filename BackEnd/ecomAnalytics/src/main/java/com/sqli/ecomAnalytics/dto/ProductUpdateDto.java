package com.sqli.ecomAnalytics.dto;

import com.sqli.ecomAnalytics.entity.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductUpdateDto {
    private String description;
    private ProductCategory category;
    private String brand;
    private BigDecimal price;
    private Integer stockQuantity;
}
