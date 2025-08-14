package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customers, Integer> {

    Optional<Customers> findByEmail(String email);

    Optional<Customers> findByCustomerCode(String customerCode);


    boolean existsByEmail(String email);

    boolean existsByCustomerCode(String customerCode);

    Customers findTopByOrderByTotalSpentDesc();

    Optional<Customers> findByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT c FROM Customers c WHERE c.totalSpent >= :minSpent ORDER BY c.totalSpent DESC")
    List<Customers> findHighSpendingCustomers(@Param("minSpent") BigDecimal minSpent);

    @Query("SELECT c FROM Customers c LEFT JOIN FETCH c.customerSegment WHERE c.customerId = :id")
    Optional<Customers> findWithSegment(@Param("id") Integer id);

    @Query("SELECT c FROM Customers c JOIN FETCH c.customerSegment cs WHERE cs.segmentLabel = :segmentLabel")
    List<Customers> findCustomersBySegment(@Param("segmentLabel") Segments segmentLabel);

    @Query("SELECT COUNT(c) FROM Customers c WHERE c.registrationDate BETWEEN :startDate AND :endDate")
    Long countCustomersRegisteredBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    // Customer Analytics

    @Query("SELECT AVG(c.totalSpent) FROM Customers c")
    BigDecimal getAverageLifetimeValue();

    @Query("SELECT cs.segmentLabel, COUNT(cs) FROM CustomerSegments cs GROUP BY cs.segmentLabel")
    List<Object[]> getCustomerCountBySegment();

    @Query("SELECT AVG(c.totalSpent) FROM Customers c WHERE c.registrationDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageCustomerValueByPeriod(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT EXTRACT(YEAR FROM registration_date) as year, " +
            "EXTRACT(MONTH FROM registration_date) as month, " +
            "COUNT(*) as count " +
            "FROM customers " +
            "WHERE registration_date BETWEEN :startDate AND :endDate " +
            "GROUP BY EXTRACT(YEAR FROM registration_date), EXTRACT(MONTH FROM registration_date) " +
            "ORDER BY year, month",
            nativeQuery = true)
    List<Object[]> getMonthlyRegistrationTrends(@Param("startDate") LocalDateTime startDate,@Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(c) FROM Customers c WHERE c.lastOrderDate < :thresholdDate")
    Long countChurnedCustomers(@Param("thresholdDate") LocalDateTime thresholdDate);

    @Query("SELECT COUNT(c) FROM Customers c")
    Long countAllCustomers();
}

