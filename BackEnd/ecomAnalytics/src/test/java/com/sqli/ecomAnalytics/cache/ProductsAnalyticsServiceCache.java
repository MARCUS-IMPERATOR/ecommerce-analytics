package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.Analytics.ProductsAnalyticsService;
import com.sqli.ecomAnalytics.dto.ProductPerformanceDto;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import com.sqli.ecomAnalytics.util.RedisCacheKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProductsAnalyticsServiceCache extends BaseCacheTest {

    @Autowired
    private ProductsAnalyticsService productsAnalyticsService;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void getProductPerformanceCache() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 25, 23, 59);
        int lowStockThreshold = 10;

        Products product = new Products();
        product.setProductId(1);
        product.setName("Laptop");

        Object[] topProduct = new Object[]{product, 50L, new BigDecimal("50000.00")};
        when(productRepository.getProductPerformanceByRevenue(start, end)).thenReturn(Collections.singletonList(topProduct));

        Object[] categoryPerf = new Object[]{"Electronics", 100L, new BigDecimal("75000.00"), 5};
        when(productRepository.getCategoryPerformance(start, end))
                .thenReturn(Collections.singletonList(categoryPerf));

        Object[] inventoryData = new Object[]{1, "Laptop", 25, 50L};
        when(productRepository.getInventoryTurnoverData(start, end))
                .thenReturn(Collections.singletonList(inventoryData));

        Products lowStockProduct = new Products();
        lowStockProduct.setProductId(2);
        lowStockProduct.setName("Mouse");
        when(productRepository.findLowStock(lowStockThreshold))
                .thenReturn(Collections.singletonList(lowStockProduct));

        ProductPerformanceDto result1 = productsAnalyticsService.getProductPerformance(start, end, lowStockThreshold);

        String cacheKey = RedisCacheKeys.productPerformanceKeys(start, end, lowStockThreshold);
        assertCache("productsPerformanceCache", cacheKey, result1);

        ProductPerformanceDto result2 = productsAnalyticsService.getProductPerformance(start, end, lowStockThreshold);

        assertThat(result1).isEqualTo(result2);

        verify(productRepository, times(1)).getProductPerformanceByRevenue(start, end);
        verify(productRepository, times(1)).getCategoryPerformance(start, end);
        verify(productRepository, times(1)).getInventoryTurnoverData(start, end);
        verify(productRepository, times(1)).findLowStock(lowStockThreshold);
    }
}
