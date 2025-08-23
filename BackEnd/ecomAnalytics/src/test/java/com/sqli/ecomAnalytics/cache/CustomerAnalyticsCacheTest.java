package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.Analytics.CustomersAnalyticsService;
import com.sqli.ecomAnalytics.dto.CustomerAnalyticsDto;
import com.sqli.ecomAnalytics.entity.CustomerSegments;
import com.sqli.ecomAnalytics.entity.Segments;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.util.RedisCacheKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CustomerAnalyticsCacheTest extends BaseCacheTest{

    @Autowired
    private CustomersAnalyticsService analyticsService;

    @MockitoBean
    private CustomerRepository customerRepository;


    @BeforeEach
    void setUp() {
        clearAllCaches();
        reset(customerRepository);
        Cache cache = cacheManager.getCache("customersAnalyticsCache");
    }

    @Test
    void getAnalyticsCache() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 25, 23, 59);
        LocalDateTime threshold = LocalDateTime.of(2025, 8, 19, 0, 0);

        CustomerSegments cs = new CustomerSegments();
        cs.setSegmentLabel(Segments.CHAMPION);
        cs.setCustomerId(1);
        cs.setRecency(1);
        cs.setFrequency(new BigDecimal("5.0"));
        cs.setMonetary(new BigDecimal("1000.0"));

        when(customerRepository.getCustomerCountBySegment())
                .thenReturn(Collections.singletonList(new Object[]{cs, 100L}));
        when(customerRepository.findHighSpendingCustomers(any()))
                .thenReturn(Collections.emptyList());
        when(customerRepository.getAverageLifetimeValue())
                .thenReturn(new BigDecimal("1500.00"));
        when(customerRepository.getMonthlyRegistrationTrends(any(), any()))
                .thenReturn(Collections.emptyList());
        when(customerRepository.countChurnedCustomers(any()))
                .thenReturn(50L);
        when(customerRepository.countAllCustomers())
                .thenReturn(1000L);

        String cacheKey = RedisCacheKeys.customerAnalyticsKey(start, end, threshold);

        Cache cache = cacheManager.getCache("customersAnalyticsCache");
        Cache.ValueWrapper existingValue = cache != null ? cache.get(cacheKey) : null;

        CustomerAnalyticsDto r1 = null;
        try {
            r1 = analyticsService.getAnalytics(start, end, threshold);
        } catch (Exception e) {
            System.err.println("Exception during first call: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        assertThat(r1).isNotNull();
        assertThat(r1.getSegmentDistribution()).containsEntry("CHAMPION", 100L);

        Cache.ValueWrapper cachedValue = cache.get(cacheKey);
        if (cachedValue != null) {
            System.out.println("Cached value equals result: " + r1.equals(cachedValue.get()));
        }

        try {
            assertCache("customersAnalyticsCache", cacheKey, r1);
        } catch (AssertionError e) {
            System.err.println("Cache assertion failed: " + e.getMessage());
            throw e;
        }
        CustomerAnalyticsDto r2 = null;
        try {
            System.out.println("Making second call to analyticsService.getAnalytics()...");
            r2 = analyticsService.getAnalytics(start, end, threshold);
            System.out.println("Second call completed successfully");
            System.out.println("Same instance returned: " + (r1 == r2));
        } catch (Exception e) {
            System.err.println("Exception during second call: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        assertThat(r1).isEqualTo(r2);

        try {
            verify(customerRepository, times(1)).getCustomerCountBySegment();
        } catch (MockitoAssertionError e) {
            System.err.println(" verification failed: " + e.getMessage());
            try {
                verify(customerRepository, atLeastOnce()).getCustomerCountBySegment();
            } catch (MockitoAssertionError e2) {
                System.out.println(mockingDetails(customerRepository).getInvocations());
            }
            throw e;
        }

        verify(customerRepository, times(1)).findHighSpendingCustomers(any());
        verify(customerRepository, times(1)).getAverageLifetimeValue();
        verify(customerRepository, times(1)).getMonthlyRegistrationTrends(any(), any());
        verify(customerRepository, times(1)).countChurnedCustomers(any());
        verify(customerRepository, times(1)).countAllCustomers();
    }

    @Test
    void testDirectRepositoryCall() {
        CustomerSegments cs = new CustomerSegments();
        cs.setSegmentLabel(Segments.CHAMPION);

        when(customerRepository.getCustomerCountBySegment())
                .thenReturn(Collections.singletonList(new Object[]{cs, 100L}));

        List<Object[]> result = customerRepository.getCustomerCountBySegment();

        verify(customerRepository, times(1)).getCustomerCountBySegment();
    }
}
