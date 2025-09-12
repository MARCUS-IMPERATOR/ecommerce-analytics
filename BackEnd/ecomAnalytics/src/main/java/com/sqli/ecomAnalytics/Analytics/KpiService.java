package com.sqli.ecomAnalytics.Analytics;

import com.sqli.ecomAnalytics.dto.KpiDto;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class KpiService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public KpiService(OrderRepository orderRepository, CustomerRepository customerRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    private BigDecimal getTotalRevenue(){
        return orderRepository.findTotalRevenue();
    }

    private BigDecimal getAverageOrderValue(LocalDateTime start, LocalDateTime end) {
        return orderRepository.getAverageOrderValue(start, end);
    }

    private Long newCustomers(LocalDateTime start, LocalDateTime end) {
        return customerRepository.countCustomersRegisteredBetween(start, end);
    }

    private Long countCustomers(){
        return customerRepository.countAllCustomers();
    }

    private Long countOrders(){
        return orderRepository.countAllOrders();
    }

    private Long countProducts(){
        return productRepository.countAllProducts();
    }

    @Cacheable(value = "kpiCache",  key = "T(com.sqli.ecomAnalytics.util.RedisCacheKeys).kpiKeys(#start,#end)")
    @Transactional(readOnly = true)
    public KpiDto getKpi(LocalDateTime start, LocalDateTime end) {
        KpiDto kpi = new KpiDto();
        kpi.setAverageOrderValue(getAverageOrderValue(start, end));
        kpi.setTotalRevenue(getTotalRevenue());
        kpi.setPeriodStart(start);
        kpi.setPeriodEnd(end);
        kpi.setTotalCustomers(countCustomers());
        kpi.setTotalProducts(countProducts());
        kpi.setNewCustomersCount(newCustomers(start, end));
        kpi.setTotalOrders(countOrders());

        return kpi;
    }
}
