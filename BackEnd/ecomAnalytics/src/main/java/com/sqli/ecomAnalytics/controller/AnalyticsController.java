package com.sqli.ecomAnalytics.controller;

import com.sqli.ecomAnalytics.Analytics.CustomersAnalyticsService;
import com.sqli.ecomAnalytics.Analytics.KpiService;
import com.sqli.ecomAnalytics.Analytics.ProductsAnalyticsService;
import com.sqli.ecomAnalytics.Analytics.SalesAnalyticsService;
import com.sqli.ecomAnalytics.dto.CustomerAnalyticsDto;
import com.sqli.ecomAnalytics.dto.KpiDto;
import com.sqli.ecomAnalytics.dto.ProductPerformanceDto;
import com.sqli.ecomAnalytics.dto.SalesTrendDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final CustomersAnalyticsService  customersAnalyticsService;
    private final KpiService  kpiService;
    private final ProductsAnalyticsService productsAnalyticsService;
    private final SalesAnalyticsService  salesAnalyticsService;

    public AnalyticsController(CustomersAnalyticsService customersAnalyticsService, KpiService kpiService, ProductsAnalyticsService productsAnalyticsService, SalesAnalyticsService salesAnalyticsService) {
        this.customersAnalyticsService = customersAnalyticsService;
        this.kpiService = kpiService;
        this.productsAnalyticsService = productsAnalyticsService;
        this.salesAnalyticsService = salesAnalyticsService;
    }

    @Operation(summary = "Get customers related analytics", responses = {@ApiResponse(responseCode = "200", description = "List of customers returned successfully")})
    @GetMapping("/customerAnalytics/start/{startTime}/end/{endTime}/threshold/{threshold}")
    public ResponseEntity<CustomerAnalyticsDto> getAnalytics(
            @Parameter(description = "")
            @PathVariable LocalDateTime startTime,
            @Parameter(description = "")
            @PathVariable LocalDateTime endTime,
            @Parameter(description = "")
            @PathVariable LocalDateTime threshold
            ){
        return ResponseEntity.ok(customersAnalyticsService.getAnalytics(startTime, endTime, threshold));
    }

    @Operation(summary = "Get KPI", responses = {@ApiResponse(responseCode = "200", description = "KPI dto is returned")})
    @GetMapping("/kpi/start/{startTime}/end/{endTime}")
    public ResponseEntity<KpiDto> getKpi(
            @Parameter(description = "")
            @PathVariable LocalDateTime startTime,
            @Parameter(description = "")
            @PathVariable LocalDateTime endTime
    ){
        return ResponseEntity.ok(kpiService.getKpi(startTime, endTime));
    }

    @Operation(summary = "Get products performance", responses = {@ApiResponse(responseCode = "200", description = "Products performance dto is returned")})
    @GetMapping("/productsPerformance/start/{startTime}/end/{endTime}/threshold/{threshold}")
    public ResponseEntity<ProductPerformanceDto> getProductPerformance(
            @Parameter(description = "")
            @PathVariable LocalDateTime startTime,
            @Parameter(description = "")
            @PathVariable LocalDateTime endTime,
            @Parameter(description = "")
            @PathVariable int threshold
    ){
        return ResponseEntity.ok(productsAnalyticsService.getProductPerformance(startTime, endTime, threshold));
    }

    @Operation(summary = "Get sales analytics", responses = {@ApiResponse(responseCode = "200", description = "Sales analytics dto is returned")})
    @GetMapping("/salesAnalytics/start/{startTime}/end/{endTime}/")
    public ResponseEntity<SalesTrendDto> getSalesTrend(
            @Parameter(description = "")
            @PathVariable LocalDateTime startTime,
            @Parameter(description = "")
            @PathVariable LocalDateTime endTime
    ){
        return ResponseEntity.ok(salesAnalyticsService.getsalesTrend(startTime,endTime));
    }

}
