package com.sqli.ecomAnalytics.analytics;

import com.sqli.ecomAnalytics.Analytics.KpiService;
import com.sqli.ecomAnalytics.dto.KpiDto;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KpiServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private KpiService kpiService;

    @Test
    void getKpi() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(orderRepository.findTotalRevenue()).thenReturn(new BigDecimal("100000.00"));
        when(orderRepository.getAverageOrderValue(start, end)).thenReturn(new BigDecimal("75.50"));
        when(orderRepository.countAllOrders()).thenReturn(5000L);
        when(customerRepository.countAllCustomers()).thenReturn(1500L);
        when(customerRepository.countCustomersRegisteredBetween(start, end)).thenReturn(300L);

        KpiDto result = kpiService.getKpi(start, end);

        // Then
        assertThat(result.getTotalRevenue()).isEqualTo("100000.00");
        assertThat(result.getAverageOrderValue()).isEqualTo("75.50");
        assertThat(result.getTotalOrders()).isEqualTo(5000L);
        assertThat(result.getTotalCustomers()).isEqualTo(1500L);
        assertThat(result.getNewCustomersCount()).isEqualTo(300L);
        assertThat(result.getPeriodStart()).isEqualTo(start);
        assertThat(result.getPeriodEnd()).isEqualTo(end);
    }
}
