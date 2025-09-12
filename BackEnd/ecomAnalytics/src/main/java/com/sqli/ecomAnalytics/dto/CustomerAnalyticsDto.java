package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAnalyticsDto {
    private Map<String, Long> segmentDistribution;
    private List<TopCustomerData> topCustomers;
    private BigDecimal averageCustomerLifetimeValue;
    private List<CustomerRegistrationTrendData> registrationTrends;
    private BigDecimal churnRate;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopCustomerData {
        private int customerId;
        private String customerName;
        private BigDecimal totalSpent;
        private int orderCount;
        private String segment;
        private BigDecimal ltv;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerRegistrationTrendData {
        private int year;
        private int month;
        private Long registrationCount;
    }
}
