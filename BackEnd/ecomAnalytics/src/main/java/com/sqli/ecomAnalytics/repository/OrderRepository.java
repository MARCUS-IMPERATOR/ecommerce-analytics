package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.OrderStatus;
import com.sqli.ecomAnalytics.entity.Orders;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrderRepository extends JpaRepository<Orders, Integer> {
    List<Orders> findByStatus(OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Orders o WHERE o.customer.customerId = :customerId AND o.status = 'DELIVERED'")
    Optional<BigDecimal> getTotalSpentByCustomer(@Param("customerId") Integer customerId);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.customer.customerId = :customerId AND o.status = 'DELIVERED'")
    int getOrderCountByCustomer(@Param("customerId") Integer customerId);


    //KPI

    @Query("SELECT SUM(o.totalAmount) FROM Orders o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED'")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Orders o WHERE o.status = 'DELIVERED'")
    BigDecimal findTotalRevenue();

    @Query("SELECT COUNT(o.orderId) FROM Orders o WHERE o.status = 'DELIVERED'")
    Long findCountOrders();

    @Query(value = "SELECT DATE(order_date) as orderDate, " +
            "SUM(total_amount) as dailyRevenue, " +
            "COUNT(*) as orderCount " +
            "FROM orders " +
            "WHERE order_date BETWEEN :startDate AND :endDate AND status = 'DELIVERED' " +
            "GROUP BY DATE(order_date) " +
            "ORDER BY orderDate", nativeQuery = true)
    List<Object[]> getDailySalesTrends(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(o.totalAmount) FROM Orders o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED'")
    BigDecimal getAverageOrderValue(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.status, COUNT(o) FROM Orders o WHERE o.orderDate BETWEEN :startDate AND :endDate GROUP BY o.status")
    List<Object[]> getOrderStatusDistribution(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.customer, SUM(o.totalAmount) as totalSpent FROM Orders o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'DELIVERED' " +
            "GROUP BY o.customer ORDER BY totalSpent DESC")
    List<Object[]> getTopCustomersByRevenue(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate,Pageable pageable);

    @Query("SELECT COUNT(o) FROM Orders o")
    Long countAllOrders();
}
