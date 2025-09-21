package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.OrderItems;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Integer> {
    List<OrderItems> findByOrderOrderId(Integer orderId);

    @Query("SELECT oi FROM OrderItems oi WHERE oi.product.productId = :productId")
    List<OrderItems> findByProductId(@Param("productId") Integer productId);

    @Query("SELECT oi.product, SUM(oi.quantity) as totalSold FROM OrderItems oi " +
            "GROUP BY oi.product ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
}
