package com.sqli.ecomAnalytics.Analytics;

import com.sqli.ecomAnalytics.dto.SalesTrendDto;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesAnalyticsService {
    private final OrderRepository orderRepository;

    public SalesAnalyticsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private List<SalesTrendDto.DailySalesData> fetchDailySales(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> dailyRaw = orderRepository.getDailySalesTrends(startDate, endDate);

        return dailyRaw.stream()
                .map(record -> new SalesTrendDto.DailySalesData(
                        ((java.sql.Date) record[0]).toLocalDate(),
                        (BigDecimal) record[1],
                        ((Number) record[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    private List<SalesTrendDto.MonthlySalesData> aggregateMonthlySales(List<SalesTrendDto.DailySalesData> dailySales) {
        Map<YearMonth, List<SalesTrendDto.DailySalesData>> groupedByMonth = dailySales.stream()
                .collect(Collectors.groupingBy(d -> YearMonth.from(d.getDate())));

        return groupedByMonth.entrySet().stream()
                .map(entry -> {
                    YearMonth ym = entry.getKey();
                    BigDecimal totalRevenue = entry.getValue().stream()
                            .map(SalesTrendDto.DailySalesData::getRevenue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Long totalOrders = entry.getValue().stream()
                            .mapToLong(SalesTrendDto.DailySalesData::getOrderCount)
                            .sum();

                    return new SalesTrendDto.MonthlySalesData(
                            ym.getYear(),
                            ym.getMonthValue(),
                            totalRevenue,
                            totalOrders
                    );
                })
                .sorted(Comparator.comparing(m -> YearMonth.of(m.getYear(), m.getMonth())))
                .collect(Collectors.toList());
    }


    private String determineTrend(List<SalesTrendDto.MonthlySalesData> monthlySales) {
        if (monthlySales.size() < 2) {
            return "STABLE";
        }

        BigDecimal lastMonthRevenue = monthlySales.get(monthlySales.size() - 1).getRevenue();
        BigDecimal prevMonthRevenue = monthlySales.get(monthlySales.size() - 2).getRevenue();

        if (lastMonthRevenue.compareTo(prevMonthRevenue) > 0) {
            return "UP";
        } else if (lastMonthRevenue.compareTo(prevMonthRevenue) < 0) {
            return "DOWN";
        } else {
            return "STABLE";
        }
    }

    @Cacheable(value = "salesTrendCache",  key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).salesTrendKeys(#startDate, #endDate)")
    @Transactional(readOnly = true)
    public SalesTrendDto getsalesTrend(LocalDateTime startDate, LocalDateTime endDate) {
        List<SalesTrendDto.DailySalesData>  dailySales = fetchDailySales(startDate, endDate);
        List<SalesTrendDto.MonthlySalesData>  monthlySales = aggregateMonthlySales(dailySales);
        String trend = determineTrend(monthlySales);

        return new SalesTrendDto(dailySales,monthlySales,trend);
    }
}
