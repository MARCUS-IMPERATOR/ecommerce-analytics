package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.ProductRecommendations;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRecommendationsRepository extends JpaRepository<ProductRecommendations, Integer> {
    //Research both of those queries
    @Query("SELECT pr FROM ProductRecommendations pr WHERE pr.customerId = :customerId ORDER BY pr.score DESC")
    List<ProductRecommendations> findTopRecommendationsForCustomer(@Param("customerId") Integer customerId,
                                                                   Pageable pageable);

    @Query("SELECT pr FROM ProductRecommendations pr WHERE pr.customerId = :customerId AND pr.score >= :minScore")
    List<ProductRecommendations> findHighScoreRecommendations(@Param("customerId") Integer customerId, @Param("minScore") BigDecimal minScore);

    @Query("SELECT pr.productId, pr.product.name, COUNT(pr), AVG(pr.score) " +
            "FROM ProductRecommendations pr " +
            "GROUP BY pr.productId, pr.product.name " +
            "ORDER BY COUNT(pr) DESC")
    List<Object[]> findTopRecommendedProducts();

    @Query("SELECT COUNT(DISTINCT pr.customerId) FROM ProductRecommendations pr")
    long countCustomersWithRecommendations();

    @Query("SELECT AVG(sub.count) " +
            "FROM (SELECT COUNT(pr) as count FROM ProductRecommendations pr GROUP BY pr.customerId) sub")
    double averageRecommendationsPerCustomer();

}
