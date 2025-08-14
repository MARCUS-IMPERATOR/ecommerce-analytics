package com.sqli.ecomAnalytics.analytics;

import com.sqli.ecomAnalytics.Analytics.SalesAnalyticsService;
import com.sqli.ecomAnalytics.dto.SalesTrendDto;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesTrendServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SalesAnalyticsService  salesAnalyticsService;

    @Test
    void getSalesTrend(){
        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 20, 23, 59);

        Object[] aug1 = new Object[] {Date.valueOf("2025-08-01"), new BigDecimal("1000.00"), 10L};
        Object[] aug15 = new Object[] {Date.valueOf("2025-08-15"), new BigDecimal("1500.00"), 15L};
        Object[] sep1 = new Object[] {Date.valueOf("2025-09-01"), new BigDecimal("2000.00"), 20L};
        Object[] oct1 = new Object[] {Date.valueOf("2025-10-01"), new BigDecimal("1800.00"), 18L};
        Object[] oct15 = new Object[] {Date.valueOf("2025-10-15"), new BigDecimal("2200.00"), 22L};

        List<Object[]> data = List.of(aug1, aug15, sep1, oct1, oct15);

        when(orderRepository.getDailySalesTrends(start,end)).thenReturn(data);

        SalesTrendDto r = salesAnalyticsService.getsalesTrend(start,end);

        assertThat(r.getDailySales()).hasSize(5);
        assertThat(r.getDailySales().get(0).getDate()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(r.getDailySales().get(0).getRevenue()).isEqualByComparingTo("1000.00");
        assertThat(r.getDailySales().get(0).getOrderCount()).isEqualTo(10L);

        assertThat(r.getMonthlySales()).hasSize(3);

        SalesTrendDto.MonthlySalesData augData = r.getMonthlySales().stream()
                .filter(m -> m.getYear() == 2025 && m.getMonth() == 8)
                .findFirst().orElseThrow();
        assertThat(augData.getRevenue()).isEqualByComparingTo("2500.00"); // 1000 + 1500
        assertThat(augData.getOrderCount()).isEqualTo(25L); // 10 + 15

        // March totals
        SalesTrendDto.MonthlySalesData octData = r.getMonthlySales().stream()
                .filter(m -> m.getYear() == 2025 && m.getMonth() == 10)
                .findFirst().orElseThrow();
        assertThat(octData.getRevenue()).isEqualByComparingTo("4000.00"); // 1800 + 2200
        assertThat(octData.getOrderCount()).isEqualTo(40L); // 18 + 22

        // Verify trend calculation (compare last two months: Feb 2000, Mar 4000 => UP)
        assertThat(r.getTrendDirection()).isEqualTo("UP");

        verify(orderRepository, times(1)).getDailySalesTrends(start, end);
    }
}
