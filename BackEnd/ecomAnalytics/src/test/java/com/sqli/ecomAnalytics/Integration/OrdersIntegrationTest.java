package com.sqli.ecomAnalytics.Integration;

import com.sqli.ecomAnalytics.entity.Customers;
import com.sqli.ecomAnalytics.entity.OrderStatus;
import com.sqli.ecomAnalytics.entity.Orders;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
public class OrdersIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ecom_analytics_test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.flyway.url", postgresContainer::getJdbcUrl);
        registry.add("spring.flyway.user", postgresContainer::getUsername);
        registry.add("spring.flyway.password", postgresContainer::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private Customers createAndSaveCustomer() {
        Customers customer = new Customers();
        customer.setCustomerCode("CUST-001");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("000000");
        customer.setAge(30);
        customer.setCountry("Morocco");
        customer.setRegistrationDate(LocalDateTime.now());
        customer.setTotalSpent(BigDecimal.ZERO);
        customer.setOrderCount(0);

        return customerRepository.saveAndFlush(customer);
    }

    @Test
    void testFindTotalRevenueAndCountOrders() {
        Customers customer = createAndSaveCustomer();

        Orders order1 = new Orders();
        order1.setCustomer(customer);
        order1.setOrderDate(LocalDateTime.of(2025,8,10,10,0));
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order1);

        Orders order2 = new Orders();
        order2.setCustomer(customer);
        order2.setOrderDate(LocalDateTime.of(2025,8,15,10,0));
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order2);

        orderRepository.flush();

        BigDecimal totalRevenue = orderRepository.findTotalRevenue();
        assertThat(totalRevenue).isEqualByComparingTo("250.00");

        long orderCount = orderRepository.countAllOrders();
        assertThat(orderCount).isEqualTo(2L);
    }

    @Test
    void testGetAverageOrderValue() {
        Customers customer = createAndSaveCustomer();

        LocalDateTime start = LocalDateTime.of(2025, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 8, 31, 23, 59);

        Orders order1 = new Orders();
        order1.setCustomer(customer);
        order1.setOrderDate(LocalDateTime.of(2025,8,10,10,0));
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order1);

        Orders order2 = new Orders();
        order2.setCustomer(customer);
        order2.setOrderDate(LocalDateTime.of(2025,8,15,10,0));
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order2);

        orderRepository.flush();

        BigDecimal avgOrderValue = orderRepository.getAverageOrderValue(start, end);
        assertThat(avgOrderValue).isEqualByComparingTo("125.00");
    }

    @Test
    void testGetDailySalesTrends() {
        Customers customer = createAndSaveCustomer();

        LocalDateTime start = LocalDateTime.of(2025,8,1,0,0);
        LocalDateTime end = LocalDateTime.of(2025,8,31,23,59);

        Orders order1 = new Orders();
        order1.setCustomer(customer);
        order1.setOrderDate(LocalDateTime.of(2025,8,10,10,0));
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order1);

        Orders order2 = new Orders();
        order2.setCustomer(customer);
        order2.setOrderDate(LocalDateTime.of(2025,8,10,15,0));
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order2);

        orderRepository.flush();

        List<Object[]> dailySales = orderRepository.getDailySalesTrends(start, end);

        assertThat(dailySales).isNotEmpty();

        Object[] firstDay = dailySales.get(0);
        assertThat(firstDay[0]).isInstanceOf(java.sql.Date.class);
        assertThat(firstDay[1]).isInstanceOf(BigDecimal.class);
        assertThat(firstDay[2]).isInstanceOf(Long.class);
    }
}
