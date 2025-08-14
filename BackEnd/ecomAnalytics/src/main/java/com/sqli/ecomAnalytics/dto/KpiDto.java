package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class KpiDto {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalCustomers;
    private BigDecimal averageOrderValue;
    private Long newCustomersCount;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
