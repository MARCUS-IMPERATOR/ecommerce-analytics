package com.sqli.ecomAnalytics.analytics;

import com.sqli.ecomAnalytics.Analytics.ProductsAnalyticsService;
import com.sqli.ecomAnalytics.dto.ProductPerformanceDto;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductAnalyticsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductsAnalyticsService productsAnalyticsService;

    private Products createMockProduct(int id, String name, BigDecimal price) {
        Products product = new Products();
        product.setProductId(id);
        product.setName(name);
        product.setPrice(price);
        return product;
    }

    @Test
    void getProductPerformance_ShouldReturnCompleteProductPerformanceDto() {

        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);
        int lowStockThreshold = 10;

        Products product = createMockProduct(1, "Laptop", new BigDecimal("999.99"));
        Object[] topProductData = new Object[] { product, 50L, new BigDecimal("49999.50") };
        List<Object[]> topProducts = Collections.singletonList(topProductData);
        when(productRepository.getProductPerformanceByRevenue(start, end)).thenReturn(topProducts);


        Object[] categoryData = new Object[] {"Electronics", 100L, new BigDecimal("75000.00"), 5};
        List<Object[]> categoryList = Collections.singletonList(categoryData);
        when(productRepository.getCategoryPerformance(start, end)).thenReturn(categoryList);

        Object[] inventoryData = new Object[] {1, "Laptop", 25, 50L};
        List<Object[]> inventoryList = Collections.singletonList(inventoryData);
        when(productRepository.getInventoryTurnoverData(start, end)).thenReturn(inventoryList);

        Products lowStockProduct = createMockProduct(2, "Mouse", new BigDecimal("29.99"));
        when(productRepository.findLowStock(lowStockThreshold))
                .thenReturn(List.of(lowStockProduct));

        ProductPerformanceDto result = productsAnalyticsService.getProductPerformance(start, end, lowStockThreshold);

        assertThat(result.getTopSellingProducts()).hasSize(1);
        assertThat(result.getTopSellingProducts().get(0).getProductName()).isEqualTo("Laptop");
        assertThat(result.getTopSellingProducts().get(0).getQuantitySold()).isEqualTo(50L);
        assertThat(result.getTopSellingProducts().get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("49999.50"));

        assertThat(result.getCategoryPerformance()).hasSize(1);
        assertThat(result.getCategoryPerformance().get(0).getCategory()).isEqualTo("Electronics");
        assertThat(result.getCategoryPerformance().get(0).getTotalQuantitySold()).isEqualTo(100L);

        assertThat(result.getInventoryAnalysis()).hasSize(1);
        assertThat(result.getInventoryAnalysis().get(0).getTurnoverRate()).isEqualTo(2.0);

        assertThat(result.getLowStockAlerts()).hasSize(1);
        assertThat(result.getLowStockAlerts().get(0).getName()).isEqualTo("Mouse");
    }
}
