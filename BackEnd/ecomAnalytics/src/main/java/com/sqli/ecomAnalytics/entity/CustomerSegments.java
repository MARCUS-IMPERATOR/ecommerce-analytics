package com.sqli.ecomAnalytics.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "customer_segments")
public class CustomerSegments extends AbstractAudit {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @MapsId
    private Customers customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_label", nullable = false)
    private Segments segmentLabel;

    @Column(name = "recency", nullable = false)
    private int recency;

    @Column(name = "frequency", nullable = false)
    private BigDecimal frequency;

    @Column(name = "monetary", nullable = false, precision = 10, scale = 2)
    private BigDecimal monetary;

    @Column(name = "segment_score")
    private int segmentScore;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;
}
