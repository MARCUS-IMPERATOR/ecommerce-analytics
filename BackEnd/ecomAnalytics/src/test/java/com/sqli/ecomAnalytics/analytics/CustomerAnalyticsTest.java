package com.sqli.ecomAnalytics.analytics;

import com.sqli.ecomAnalytics.Analytics.CustomersAnalyticsService;
import com.sqli.ecomAnalytics.dto.CustomerAnalyticsDto;
import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerAnalyticsTest {
    @Mock
    private CustomerRepository customerRepository;
    @InjectMocks
    private CustomersAnalyticsService analyticsService;

    private Customers createMockCustomer() {
        Customers c = new Customers();
        c.setCustomerId(1);
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setAge(22);
        c.setCountry("Morocco");
        c.setTotalSpent(new BigDecimal("2500.00"));
        c.setOrderCount(15);
        return c;
    }

    private List<Object[]> createMockRegistrationTrends() {
        return List.of(
                new Object[]{2024.0, 1.0, 100L},
                new Object[]{2024.0, 2.0, 120L}
        );
    }

    @Test
    void getAnalytics() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 13, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 20, 0, 0);
        LocalDateTime threshold = LocalDateTime.of(2025, 8, 30, 0, 0);

        Object[] row = new Object[]{Segments.CHAMPION, 100L};
        List<Object[]> segmentList = Collections.singletonList(row);
        when(customerRepository.getCustomerCountBySegment()).thenReturn(segmentList);

        Customers c = createMockCustomer();
        when(customerRepository.findHighSpendingCustomers(any(BigDecimal.class))).thenReturn(List.of(c));
        when(customerRepository.getAverageLifetimeValue()).thenReturn(new BigDecimal("1500.00"));
        when(customerRepository.getMonthlyRegistrationTrends(start, end)).thenReturn(createMockRegistrationTrends());
        when(customerRepository.countChurnedCustomers(threshold)).thenReturn(50L);
        when(customerRepository.countAllCustomers()).thenReturn(1000L);

        CustomerAnalyticsDto r = analyticsService.getAnalytics(start, end, threshold);

        assertThat(r).isNotNull();
        assertThat(r.getSegmentDistribution()).containsEntry("CHAMPION", 100L);
        assertThat(r.getTopCustomers()).hasSize(1);
        assertThat(r.getAverageCustomerLifetimeValue()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(r.getChurnRate()).isEqualTo(new BigDecimal("5.00"));
        assertThat(r.getRegistrationTrends()).isNotEmpty();
    }
}
