package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.ProductCategory;
import com.sqli.ecomAnalytics.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Integer> {

    Optional<Products> findBySku(String sku);
    List<Products> findByCategory(ProductCategory category);
    List<Products> findByBrandIgnoreCase(String brand);
    List<Products> findByCategoryAndBrandIgnoreCase(ProductCategory category, String brand);

    @Query("SELECT p FROM Products p WHERE p.stockQuantity <= :minQt")
    List<Products> findLowStock(@Param("minQt") int threshold);

    //GIN
    @Query(value = "SELECT * FROM products WHERE to_tsvector('english', name || ' ' || description) " +
            "@@ plainto_tsquery('english', :searchTerm)", nativeQuery = true)
    List<Products> searchByText(@Param("searchTerm") String searchTerm);

    // KPI
    @Query("SELECT p, SUM(oi.quantity) as totalSold, SUM(oi.quantity * oi.unitPrice) as totalRevenue " +
            "FROM Products p " +
            "LEFT JOIN OrderItems oi ON oi.product = p " +
            "LEFT JOIN Orders o ON oi.order = o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED' " +
            "GROUP BY p " +
            "ORDER BY totalRevenue DESC")
    List<Object[]> getProductPerformanceByRevenue(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p.category, " +
            "SUM(oi.quantity) as totalQuantitySold, " +
            "SUM(oi.quantity * oi.unitPrice) as totalRevenue, " +
            "COUNT(DISTINCT p) as productCount " +
            "FROM Products p " +
            "LEFT JOIN OrderItems oi ON oi.product = p " +
            "LEFT JOIN Orders o ON oi.order = o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED' " +
            "GROUP BY p.category " +
            "ORDER BY totalRevenue DESC")
    List<Object[]> getCategoryPerformance(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p.productId, p.name, p.stockQuantity, " +
            "COALESCE(SUM(oi.quantity), 0) as totalSold " +
            "FROM Products p " +
            "LEFT JOIN OrderItems oi ON oi.product = p " +
            "LEFT JOIN Orders o ON oi.order = o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED' " +
            "GROUP BY p.productId, p.name, p.stockQuantity")
    List<Object[]> getInventoryTurnoverData(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
