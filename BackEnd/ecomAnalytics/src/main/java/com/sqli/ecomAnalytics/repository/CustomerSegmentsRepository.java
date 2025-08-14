package com.sqli.ecomAnalytics.repository;

import com.sqli.ecomAnalytics.entity.CustomerSegments;
import com.sqli.ecomAnalytics.entity.Segments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerSegmentsRepository extends JpaRepository<CustomerSegments, Integer> {
    List<CustomerSegments> findBySegmentLabel(Segments segmentLabel);
}
