package com.sqli.ecomAnalytics.Integration;

import com.sqli.ecomAnalytics.entity.*;
import com.sqli.ecomAnalytics.repository.CustomerRepository;
import com.sqli.ecomAnalytics.repository.OrderItemsRepository;
import com.sqli.ecomAnalytics.repository.OrderRepository;
import com.sqli.ecomAnalytics.repository.ProductRepository;
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
public class ProductsIntegrationTest {
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
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemsRepository orderItemRepository;

    @AfterEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void getProductPerformanceByRevenue() {

        Products p1 = new Products();
        p1.setName("Laptop");
        p1.setSku("SKU-001");
        p1.setPrice(new BigDecimal("1500.00"));
        p1.setStockQuantity(10);
        p1.setCategory(ProductCategory.LAPTOPS);
        p1.setBrand("BrandA");
        p1 = productRepository.saveAndFlush(p1);

        Customers customer = new Customers();
        customer.setCustomerCode("CUST-001");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAge(29);
        customer.setCountry("France");
        customer.setEmail("john@example.com");
        customer.setRegistrationDate(LocalDateTime.now());
        customer.setTotalSpent(new BigDecimal("0.00"));
        customer.setOrderCount(0);
        customer = customerRepository.saveAndFlush(customer);

        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now().minusDays(10));
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order = orderRepository.saveAndFlush(order);

        OrderItems orderItem = new OrderItems();

        orderItem.setOrderId(order.getOrderId());
        orderItem.setProductId(p1.getProductId());
        orderItem.setOrder(order);
        orderItem.setProduct(p1);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("1500.00"));
        orderItemRepository.saveAndFlush(orderItem);

        List<Object[]> performance = productRepository.getProductPerformanceByRevenue(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(performance).isNotNull();
        assertThat(performance).isNotEmpty();

        Object[] first = performance.get(0);
        Products productFromQuery = (Products) first[0];
        assertThat(productFromQuery.getName()).isEqualTo("Laptop");
    }

    @Test
    void getCategoryPerformance() {
        List<Object[]> categoryPerf = productRepository.getCategoryPerformance(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(categoryPerf).isNotNull();
    }

    @Test
    void getInventoryTurnoverData() {
        List<Object[]> inventoryData = productRepository.getInventoryTurnoverData(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(inventoryData).isNotNull();
    }

    @Test
    void findLowStock() {
        Products lowStockProduct = new Products();
        lowStockProduct.setName("Mouse");
        lowStockProduct.setSku("SKU-MOUSE");
        lowStockProduct.setPrice(new BigDecimal("25.00"));
        lowStockProduct.setStockQuantity(3);
        lowStockProduct.setCategory(ProductCategory.ACCESSORIES);
        lowStockProduct.setBrand("BrandB");
        lowStockProduct = productRepository.saveAndFlush(lowStockProduct);

        List<Products> lowStock = productRepository.findLowStock(5);
        assertThat(lowStock).contains(lowStockProduct);
    }
}
