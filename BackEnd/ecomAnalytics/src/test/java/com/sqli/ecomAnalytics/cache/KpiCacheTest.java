package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.Analytics.KpiService;
import com.sqli.ecomAnalytics.dto.KpiDto;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.util.RedisCacheKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KpiCacheTest extends BaseCacheTest {

    @Autowired
    private KpiService kpiService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @Test
    void getKpiCache() {
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 25, 23, 59);

        when(orderRepository.findTotalRevenue()).thenReturn(new BigDecimal("10000.00"));
        when(orderRepository.getAverageOrderValue(start, end)).thenReturn(new BigDecimal("250.00"));
        when(customerRepository.countCustomersRegisteredBetween(start, end)).thenReturn(50L);
        when(customerRepository.countAllCustomers()).thenReturn(1000L);
        when(orderRepository.countAllOrders()).thenReturn(40L);

        KpiDto result1 = kpiService.getKpi(start, end);

        String cacheKey = RedisCacheKeys.kpiKeys(start, end);
        assertCache("kpiCache", cacheKey, result1);

        KpiDto result2 = kpiService.getKpi(start, end);

        assertThat(result1).isEqualTo(result2);

        verify(orderRepository, times(1)).findTotalRevenue();
        verify(orderRepository, times(1)).getAverageOrderValue(start, end);
        verify(customerRepository, times(1)).countCustomersRegisteredBetween(start, end);
        verify(customerRepository, times(1)).countAllCustomers();
        verify(orderRepository, times(1)).countAllOrders();
    }

}
