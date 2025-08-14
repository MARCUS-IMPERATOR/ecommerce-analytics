package com.sqli.ecomAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesTrendDto {
    private List<DailySalesData> dailySales;
    private List<MonthlySalesData> monthlySales;
    private String trendDirection;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailySalesData {
        private LocalDate date;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlySalesData {
        private int year;
        private int month;
        private BigDecimal revenue;
        private Long orderCount;
    }
}
