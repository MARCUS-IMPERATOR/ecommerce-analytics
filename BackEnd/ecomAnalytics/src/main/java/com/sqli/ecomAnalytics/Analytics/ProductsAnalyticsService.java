package com.sqli.ecomAnalytics.Analytics;

import com.sqli.ecomAnalytics.dto.ProductPerformanceDto;
import com.sqli.ecomAnalytics.entity.Products;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductsAnalyticsService {

    private final ProductRepository productRepository;

    public ProductsAnalyticsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private List<ProductPerformanceDto.TopProductData> getTopProducts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> topProducts = productRepository.getProductPerformanceByRevenue(start, end);

        return topProducts.stream().map(obj -> {
            ProductPerformanceDto.TopProductData data = new ProductPerformanceDto.TopProductData();
            Products product = (Products) obj[0];
            Long quantitySold = (Long) obj[1];
            BigDecimal revenue = (BigDecimal) obj[2];

            data.setProductId(product.getProductId());
            data.setProductName(product.getName());
            data.setQuantitySold(quantitySold != null ? quantitySold : 0L);
            data.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);
            return data;
        }).collect(Collectors.toList());
    }

    public List<ProductPerformanceDto.CategoryPerformanceData> getCategoryPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = productRepository.getCategoryPerformance(startDate, endDate);

        return results.stream().map(record -> {
            String category = (String) record[0];
            Long totalQuantitySold = ((Number) record[1]).longValue();
            BigDecimal totalRevenue = (BigDecimal) record[2];
            int productCount = ((Number) record[3]).intValue();

            BigDecimal averagePrice = totalQuantitySold != 0 ?
                    totalRevenue.divide(BigDecimal.valueOf(totalQuantitySold), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            return new ProductPerformanceDto.CategoryPerformanceData(
                    category,
                    totalQuantitySold,
                    totalRevenue,
                    productCount,
                    averagePrice
            );
        }).collect(Collectors.toList());
    }
    public List<ProductPerformanceDto.InventoryTurnoverData> getInventoryTurnover(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = productRepository.getInventoryTurnoverData(startDate, endDate);

        return results.stream().map(record -> {
            int productId = (Integer) record[0];
            String productName = (String) record[1];
            int currentStock = (Integer) record[2];
            Long totalSold = record[3] != null ? ((Number) record[3]).longValue() : 0L;

            double turnoverRate = currentStock > 0 ? totalSold.doubleValue() / currentStock : 0.0;

            return new ProductPerformanceDto.InventoryTurnoverData(
                    productId,
                    productName,
                    currentStock,
                    totalSold,
                    turnoverRate
            );
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "productsPerformanceCache",  key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).productPerformanceKeys(#startDate,#endDate,#lowStockThreshold)")
    @Transactional(readOnly = true)
    public ProductPerformanceDto getProductPerformance(LocalDateTime startDate, LocalDateTime endDate, int lowStockThreshold) {
        List<ProductPerformanceDto.TopProductData> topSelling = getTopProducts(startDate, endDate);
        List<ProductPerformanceDto.CategoryPerformanceData> categoryPerf = getCategoryPerformance(startDate, endDate);
        List<ProductPerformanceDto.InventoryTurnoverData> inventory = getInventoryTurnover(startDate, endDate);
        List<Products> lowStockProducts = productRepository.findLowStock(lowStockThreshold);

        return new ProductPerformanceDto(topSelling, categoryPerf, inventory, lowStockProducts);
    }
}
