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
        // Clear ALL caches before each test
        clearAllCaches();

        // Reset all mock interactions
        reset(customerRepository);

        // Debug: Print cache status
        System.out.println("=== SETUP ===");
        System.out.println("Available caches: " + cacheManager.getCacheNames());
        Cache cache = cacheManager.getCache("customersAnalyticsCache");
        System.out.println("customersAnalyticsCache exists: " + (cache != null));
    }

    @Test
    void getAnalyticsCache() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 25, 23, 59);
        LocalDateTime threshold = LocalDateTime.of(2025, 8, 19, 0, 0);

        System.out.println("=== TEST START ===");
        System.out.println("Test parameters: start=" + start + ", end=" + end + ", threshold=" + threshold);

        // Create CustomerSegments entity
        CustomerSegments cs = new CustomerSegments();
        cs.setSegmentLabel(Segments.CHAMPION);
        cs.setCustomerId(1);
        cs.setRecency(1);
        cs.setFrequency(new BigDecimal("5.0"));
        cs.setMonetary(new BigDecimal("1000.0"));

        // Setup mocks with detailed logging
        System.out.println("Setting up mocks...");
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

        // Check cache before first call
        String cacheKey = RedisCacheKeys.customerAnalyticsKey(start, end, threshold);
        System.out.println("Generated cache key: " + cacheKey);

        Cache cache = cacheManager.getCache("customersAnalyticsCache");
        Cache.ValueWrapper existingValue = cache != null ? cache.get(cacheKey) : null;
        System.out.println("Existing cached value: " + existingValue);

        CustomerAnalyticsDto r1 = null;
        try {
            System.out.println("Making first call to analyticsService.getAnalytics()...");
            r1 = analyticsService.getAnalytics(start, end, threshold);
            System.out.println("First call completed successfully");
            System.out.println("Result: " + r1);
        } catch (Exception e) {
            System.err.println("Exception during first call: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        assertThat(r1).isNotNull();
        assertThat(r1.getSegmentDistribution()).containsEntry("CHAMPION", 100L);

        // Check if value was cached
        Cache.ValueWrapper cachedValue = cache.get(cacheKey);
        System.out.println("Value cached after first call: " + (cachedValue != null));
        if (cachedValue != null) {
            System.out.println("Cached value equals result: " + r1.equals(cachedValue.get()));
        }

        // Verify cache manually
        try {
            assertCache("customersAnalyticsCache", cacheKey, r1);
            System.out.println("Cache assertion passed");
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

        // Print mock interactions for debugging
        System.out.println("=== MOCK VERIFICATION ===");
        try {
            verify(customerRepository, times(1)).getCustomerCountBySegment();
            System.out.println("✓ getCustomerCountBySegment called exactly once");
        } catch (MockitoAssertionError e) {
            System.err.println("✗ getCustomerCountBySegment verification failed: " + e.getMessage());

            // Check if method was called at all
            try {
                verify(customerRepository, atLeastOnce()).getCustomerCountBySegment();
                System.out.println("Method was called at least once");
            } catch (MockitoAssertionError e2) {
                System.err.println("Method was never called");

                // Print all interactions
                System.out.println("All interactions with customerRepository:");
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
        // Test if the mock itself works
        System.out.println("=== DIRECT MOCK TEST ===");

        CustomerSegments cs = new CustomerSegments();
        cs.setSegmentLabel(Segments.CHAMPION);

        when(customerRepository.getCustomerCountBySegment())
                .thenReturn(Collections.singletonList(new Object[]{cs, 100L}));

        List<Object[]> result = customerRepository.getCustomerCountBySegment();
        System.out.println("Direct repository call result: " + result);

        verify(customerRepository, times(1)).getCustomerCountBySegment();
        System.out.println("Direct mock verification passed");
    }
}
