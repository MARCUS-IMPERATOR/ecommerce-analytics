package com.sqli.ecomAnalytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "product_recommendations")
@IdClass(ProductRecommendationsId.class)
public class ProductRecommendations extends AbstractAudit {
    @Id
    @Column(name = "customer_id")
    private int customerId;

    @Id
    @Column(name = "product_id")
    private int productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customers customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Products product;

    @Column(name = "score", nullable = false, precision = 10, scale = 2)
    private BigDecimal score;
}
