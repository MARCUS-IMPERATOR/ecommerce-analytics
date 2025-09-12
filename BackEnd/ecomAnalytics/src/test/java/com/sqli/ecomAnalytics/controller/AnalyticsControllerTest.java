package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.Analytics.*;
import com.sqli.ecomAnalytics.configuration.SecurityConfig;
import com.sqli.ecomAnalytics.dto.CustomerAnalyticsDto;
import com.sqli.ecomAnalytics.dto.KpiDto;
import com.sqli.ecomAnalytics.dto.ProductPerformanceDto;
import com.sqli.ecomAnalytics.dto.SalesTrendDto;
import com.sqli.ecomAnalytics.entity.Products;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
public class AnalyticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private CustomersAnalyticsService customersAnalyticsService;

    @MockitoBean
    private KpiService  kpiService;

    @MockitoBean
    private ProductsAnalyticsService  productsAnalyticsService;

    @MockitoBean
    private SalesAnalyticsService salesAnalyticsService;

    @MockitoBean
    private RecommendationAnalyticsService recommendationAnalyticsService;

    private CustomerAnalyticsDto createMockCustomerAnalyticsDto() {
        CustomerAnalyticsDto dto = new CustomerAnalyticsDto();
        dto.setSegmentDistribution(Map.of("CHAMPION", 100L, "REGULAR", 200L));
        dto.setAverageCustomerLifetimeValue(new BigDecimal("1500.00"));
        dto.setChurnRate(new BigDecimal("5.00"));
        dto.setTopCustomers(List.of(
                new CustomerAnalyticsDto.TopCustomerData(1, "John Doe", new BigDecimal("2500.00"), 15, "CHAMPION", new BigDecimal(100))
        ));
        dto.setRegistrationTrends(List.of(
                new CustomerAnalyticsDto.CustomerRegistrationTrendData(2024, 1, 100L)
        ));
        return dto;
    }

    private KpiDto createMockKpiDto() {
        KpiDto dto = new KpiDto();
        dto.setTotalRevenue(new BigDecimal("50000.00"));
        dto.setTotalCustomers(1000L);
        dto.setTotalOrders(5000L);
        dto.setAverageOrderValue(new BigDecimal("10.00"));
        dto.setNewCustomersCount(100L);
        return dto;
    }

    private ProductPerformanceDto createMockProductPerformanceDto() {
        ProductPerformanceDto.TopProductData topProduct = new ProductPerformanceDto.TopProductData();
        topProduct.setProductId(1);
        topProduct.setProductName("Laptop");
        topProduct.setQuantitySold(50L);
        topProduct.setRevenue(new BigDecimal("49999.50"));

        ProductPerformanceDto.CategoryPerformanceData categoryPerformance = new ProductPerformanceDto.CategoryPerformanceData(
                "Electronics", 100L, new BigDecimal("75000.00"), 5, new BigDecimal("750.00")
        );

        ProductPerformanceDto.InventoryTurnoverData inventoryTurnover = new ProductPerformanceDto.InventoryTurnoverData(
                1, "Laptop", 25, 50L, 2.0
        );

        List<Products> lowStockProducts = List.of(new Products() {{
            setProductId(2);
            setName("Mouse");
            setPrice(new BigDecimal("29.99"));
        }});

        return new ProductPerformanceDto(
                List.of(topProduct),
                List.of(categoryPerformance),
                List.of(inventoryTurnover),
                lowStockProducts
        );
    }

    private SalesTrendDto createMockSalesTrendDto() {
        SalesTrendDto.DailySalesData daily1 = new SalesTrendDto.DailySalesData(LocalDate.of(2025,8,1), new BigDecimal("1000.00"), 10L);
        SalesTrendDto.DailySalesData daily2 = new SalesTrendDto.DailySalesData(LocalDate.of(2025,8,15), new BigDecimal("1500.00"), 15L);
        SalesTrendDto.MonthlySalesData monthly1 = new SalesTrendDto.MonthlySalesData(2025, 8, new BigDecimal("2500.00"), 25L);
        return new SalesTrendDto(List.of(daily1, daily2), List.of(monthly1), "UP");
    }

    @Test
    void getAnalyticsDto() throws Exception {
        CustomerAnalyticsDto dto = createMockCustomerAnalyticsDto();
        when(customersAnalyticsService.getAnalytics(any(),any(),any())).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/customerAnalytics")
                        .param("start", "2025-08-10T00:00:00")
                        .param("end", "2025-09-22T23:59:59")
                        .param("threshold", "2025-08-18T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segmentDistribution.CHAMPION").value(100))
                .andExpect(jsonPath("$.averageCustomerLifetimeValue").value(1500.00))
                .andExpect(jsonPath("$.churnRate").value(5.00))
                .andExpect(jsonPath("$.topCustomers").isArray())
                .andExpect(jsonPath("$.registrationTrends").isArray());
    }

    @Test
    void getKpi() throws Exception {
        KpiDto kpi = createMockKpiDto();
        when(kpiService.getKpi(any(), any())).thenReturn(kpi);

        mockMvc.perform(get("/api/analytics/kpi")
                        .param("start","2025-08-10T00:00:00")
                        .param("end","2025-09-22T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(50000.00))
                .andExpect(jsonPath("$.totalCustomers").value(1000))
                .andExpect(jsonPath("$.totalOrders").value(5000));
    }

    @Test
    void getProductPerformance() throws Exception {
        ProductPerformanceDto dto = createMockProductPerformanceDto();
        when(productsAnalyticsService.getProductPerformance(any(), any(), anyInt())).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/productsPerformance")
                        .param("start","2025-08-10T00:00:00")
                        .param("end","2025-09-22T23:59:59")
                        .param("threshold","10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topSellingProducts").isArray())
                .andExpect(jsonPath("$.topSellingProducts[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.categoryPerformance").isArray())
                .andExpect(jsonPath("$.categoryPerformance[0].category").value("Electronics"))
                .andExpect(jsonPath("$.inventoryAnalysis").isArray())
                .andExpect(jsonPath("$.inventoryAnalysis[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.lowStockAlerts").isArray())
                .andExpect(jsonPath("$.lowStockAlerts[0].name").value("Mouse"));
    }

    @Test
    void getSalesTrend() throws Exception {
        SalesTrendDto dto = createMockSalesTrendDto();
        when(salesAnalyticsService.getsalesTrend(any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/salesAnalytics")
                        .param("start","2025-08-10T00:00:00")
                        .param("end","2025-09-22T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySales").isArray())
                .andExpect(jsonPath("$.dailySales[0].date").value("2025-08-01"))
                .andExpect(jsonPath("$.dailySales[0].revenue").value(1000.00))
                .andExpect(jsonPath("$.dailySales[0].orderCount").value(10))
                .andExpect(jsonPath("$.monthlySales").isArray())
                .andExpect(jsonPath("$.trendDirection").value("UP"));
    }
}
