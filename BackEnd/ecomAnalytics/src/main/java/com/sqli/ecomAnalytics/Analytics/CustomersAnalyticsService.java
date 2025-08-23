package com.sqli.ecomAnalytics.Analytics;

import com.sqli.ecomAnalytics.dto.CustomerAnalyticsDto;
import com.sqli.ecomAnalytics.entity.CustomerSegments;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomersAnalyticsService {

    private final CustomerRepository customerRepository;

    public CustomersAnalyticsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    private Map<String, Long> getSegmentDistribution() {
        return customerRepository.getCustomerCountBySegment().stream()
                .collect(Collectors.toMap(
                        row -> {
                            if (row[0] instanceof CustomerSegments) {
                                return ((CustomerSegments) row[0]).getSegmentLabel().name();
                            } else if (row[0] instanceof Segments) {
                                return ((Segments) row[0]).name();
                            } else {
                                return row[0].toString();
                            }
                        },
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private List<CustomerAnalyticsDto.TopCustomerData> getTopCustomers() {
        BigDecimal minSpent = new BigDecimal("100");
        return customerRepository.findHighSpendingCustomers(minSpent).stream()
                .map(c -> new CustomerAnalyticsDto.TopCustomerData(
                        c.getCustomerId(),
                        c.getFirstName() + " " + c.getLastName(),
                        c.getTotalSpent(),
                        c.getOrderCount()
                )).collect(Collectors.toList());
    }

    private BigDecimal getAverageCustomerLifetimeValue() {
        return customerRepository.getAverageLifetimeValue();
    }

    private List<CustomerAnalyticsDto.CustomerRegistrationTrendData> registrationTrends(LocalDateTime start, LocalDateTime end) {
        return customerRepository.getMonthlyRegistrationTrends(start, end).stream().map(
                row -> new CustomerAnalyticsDto.CustomerRegistrationTrendData(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue()
                )).collect(Collectors.toList());
    }

    private BigDecimal getChurnRate(LocalDateTime thresholdDate) {
        Long churned = customerRepository.countChurnedCustomers(thresholdDate);
        Long total = customerRepository.countAllCustomers();

        return total == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(churned * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    @Cacheable(value = "customersAnalyticsCache", key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).customerAnalyticsKey(#start,#end,#thresholdDate)")
    @Transactional(readOnly = true)
    public CustomerAnalyticsDto getAnalytics(LocalDateTime start, LocalDateTime end, LocalDateTime thresholdDate) {
        Map<String, Long> segmentDistribution = getSegmentDistribution();
        List<CustomerAnalyticsDto.TopCustomerData> topCustomers = getTopCustomers();
        List<CustomerAnalyticsDto.CustomerRegistrationTrendData> trend = registrationTrends(start, end);
        BigDecimal ACLTV = getAverageCustomerLifetimeValue();
        BigDecimal churnRate = getChurnRate(thresholdDate);

        CustomerAnalyticsDto analytics = new CustomerAnalyticsDto();
        analytics.setSegmentDistribution(segmentDistribution);
        analytics.setTopCustomers(topCustomers);
        analytics.setAverageCustomerLifetimeValue(ACLTV);
        analytics.setChurnRate(churnRate);
        analytics.setRegistrationTrends(trend);

        return analytics;
    }
}
