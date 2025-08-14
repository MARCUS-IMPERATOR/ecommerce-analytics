package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.Analytics.SalesAnalyticsService;
import com.sqli.ecomAnalytics.dto.SalesTrendDto;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.util.RedisCacheKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SalesAnalyticsServiceCache extends BaseCacheTest{
    @Autowired
    private SalesAnalyticsService salesAnalyticsService;

    @MockitoBean
    private OrderRepository orderRepository;

    @Test
    void getSalesTrendCache() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 25, 23, 59);

        Object[] day1 = new Object[] {Date.valueOf("2025-08-01"), new BigDecimal("1000.00"), 10L};
        Object[] day2 = new Object[] {Date.valueOf("2025-08-15"), new BigDecimal("1500.00"), 15L};
        List<Object[]> dailyData = List.of(day1, day2);

        when(orderRepository.getDailySalesTrends(start, end)).thenReturn(dailyData);

        SalesTrendDto result1 = salesAnalyticsService.getsalesTrend(start, end);

        String cacheKey = RedisCacheKeys.salesTrendKeys(start, end);
        assertCache("salesTrendCache", cacheKey, result1);

        SalesTrendDto result2 = salesAnalyticsService.getsalesTrend(start, end);

        assertThat(result1).isEqualTo(result2);

        verify(orderRepository, times(1)).getDailySalesTrends(start, end);
    }
}
