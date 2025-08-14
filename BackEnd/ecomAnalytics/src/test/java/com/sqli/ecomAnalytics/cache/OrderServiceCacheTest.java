package com.sqli.ecomAnalytics.cache;

import com.sqli.ecomAnalytics.entity.OrderStatus;
import com.sqli.ecomAnalytics.entity.Orders;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
import com.sqli.ecomAnalytics.service.OrdersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


public class OrderServiceCacheTest extends BaseCacheTest {
    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private OrderItemsRepository  orderItemsRepository;

    @Autowired
    private OrdersService ordersService;

    @BeforeEach
    public void setup() {
        clearAllCaches();
    }

    @Test
    @DisplayName("Should cache customer total spent")
    void cacheCustomerTotalSpent() {

        int customerId = 1;
        BigDecimal total = new BigDecimal("1500.00");

        when(orderRepository.getTotalSpentByCustomer(customerId)).thenReturn(Optional.of(total));

        Optional<BigDecimal> result1 = ordersService.getTotalSpentByCustomer(customerId);

        verify(orderRepository, times(1)).getTotalSpentByCustomer(customerId);
        assertThat(result1).isPresent();
        assertThat(result1.get()).isEqualTo(total);

        Optional<BigDecimal> result2 = ordersService.getTotalSpentByCustomer(customerId);

        verify(orderRepository, times(1)).getTotalSpentByCustomer(customerId);
        assertThat(result2.get()).isEqualTo(total);

        String expectedKey = "customer:spent:1";
        assertCache("customerSpentCache", expectedKey, total);
    }

    @Test
    @DisplayName("Should cache customer order count")
    void cacheCustomerOrderCount() {

        int customerId = 1;
        int count = 5;

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(orderRepository.getOrderCountByCustomer(customerId)).thenReturn(count);

        int r1 = ordersService.getOrderCountByCustomer(customerId);

        verify(orderRepository, times(1)).getOrderCountByCustomer(customerId);
        assertThat(r1).isEqualTo(count);

        int r2 = ordersService.getOrderCountByCustomer(customerId);

        verify(orderRepository, times(1)).getOrderCountByCustomer(customerId);
        assertThat(r2).isEqualTo(count);

        String expectedKey = "customer:orders:1";
        assertCache("customerOrderCountCache", expectedKey, count);
    }

    @Test
    @DisplayName("Should evict customer spending caches when updating order status")
    void evictCustomerSpendingCachesOnOrderStatusUpdate() {

        int customerId = 1;
        int orderId = 100;

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(orderRepository.getTotalSpentByCustomer(customerId)).thenReturn(Optional.of(new BigDecimal("1500.00")));
        when(orderRepository.getOrderCountByCustomer(customerId)).thenReturn(5);

        ordersService.getTotalSpentByCustomer(customerId);
        ordersService.getOrderCountByCustomer(customerId);

        assertCache("customerSpentCache", "customer:spent:1",new BigDecimal("1500.00"));
        assertCache("customerOrderCountCache", "customer:orders:1", 5);

        Orders o = new Orders();

        o.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Orders.class))).thenReturn(o);

        ordersService.updateOrderStatus(orderId, OrderStatus.DELIVERED);

        Cache spentCache = cacheManager.getCache("customerSpentCache");
        Cache orderCountCache = cacheManager.getCache("customerOrderCountCache");

        assertThat(spentCache.get("customer:spent:1")).isNull();
        assertThat(orderCountCache.get("customer:orders:1")).isNull();
    }

}
